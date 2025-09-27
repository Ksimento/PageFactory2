package ru.sbt.edu_power.e2e_core.smoke_layout;

import lombok.Getter;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.data.ImageProcessing;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbt.edu_power.e2e_core.layout.DataSetCollection;
import ru.sbt.edu_power.e2e_core.layout.DataSetVisualisation;
import ru.sbt.edu_power.e2e_core.layout.LayoutUtils;
import ru.sbt.edu_power.e2e_core.layout.MeasureElements;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.ElementsColor;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;
import ru.sbt.edu_power.e2e_core.smoke.Roles;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.DataVolume;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ScreenShotCollection {
    private static final ScreenShotCollection INSTANCE = new ScreenShotCollection();
    private final Map<Class<? extends Page>, List<Item>> pageToScreenShotCollection = new HashMap<>();
    public static final String REPORT_PATH = System.getProperty("target.directory") + File.separator +
                                             "layout_testing_cover_report";

    private ScreenShotCollection() {
    }

    public static ScreenShotCollection getINSTANCE() {
        return INSTANCE;
    }

    public Map<Class<? extends Page>, List<Item>> getPageToScreenShotCollection() {
        return pageToScreenShotCollection;
    }

    public void create(final Class<? extends Page> page, final String measuringTypes, final List<String> elements) {
        if (!pageToScreenShotCollection.containsKey(page)) {
            pageToScreenShotCollection.put(page, new ArrayList<>());
        }
        final List<Item> items = takeScreenShots(measuringTypes, elements);
        pageToScreenShotCollection.get(page).addAll(items);
    }

    private List<Item> takeScreenShots(final String measuringTypes, final List<String> elements) {
        final Map<WebElement, String> webElements = elements
                .stream()
                .collect(Collectors.toMap(FindUtils::getElementByNameOrPath, e -> e, (a, b) -> b));
        final List<Item> items = new ArrayList<>();
        while (!webElements.isEmpty()) {
            final List<WebElement> visibleElements = webElements
                    .keySet()
                    .stream()
                    .sorted((a, b) -> Mover
                            .getElementVisibleSquarePercent(b)
                            .compareTo(Mover.getElementVisibleSquarePercent(a)))
                    .filter(we -> Mover.getElementVisibleSquarePercent(we) > 50)
                    .collect(Collectors.toList());
            if (visibleElements.isEmpty()) {
                webElements.keySet()
                           .stream()
                           .min(Comparator.comparingInt(e -> e.getLocation().getY()))
                           .ifPresent(e -> scroll(Collections.singleton(e)));
                continue;
            }
            final DataSetCollection dataSetElements = new DataSetCollection();
            visibleElements.forEach(e -> dataSetElements.addAll(
                    MeasureElements.getDataSetRepositoryList(
                            e, null, MeasuringTypes.POSITION, "element"
                    )
                    )
            );
            DataSetVisualisation.setGlobalContainer();
            DataSetVisualisation.addElementsToScreen(dataSetElements, ElementsColor.RED);
            final String imgId = UUID.randomUUID().toString();
            final Path path = Paths.get(
                    REPORT_PATH,
                    "images",
                    imgId + ".png"
            );
            try {
                Files.createDirectories(path.getParent());
                final byte[] image = ImageProcessing.takeScreenShot();
                final DimensionEnum currentDimension = LayoutUtils.getCurrentDimension();
                final BufferedImage resizedImage = ImageProcessing.resize(
                        currentDimension.getBodyWidth(),
                        currentDimension.getBodyHeight(),
                        ImageIO.read(new ByteArrayInputStream(image))
                );
                ImageIO.write(resizedImage, "PNG", path.toFile());
            } catch (final IOException e) {
                throw new AutotestError("Не удалось записать изображение на диск", e);
            }
            DataSetVisualisation.clearVisualization();
            final Item item = new Item(
                    measuringTypes,
                    visibleElements.stream()
                                   .map(webElements::get)
                                   .collect(Collectors.toList()),
                    imgId

            );
            items.add(item);
            visibleElements.forEach(webElements::remove);
            if (!webElements.isEmpty()) {
                scroll(webElements.keySet());
            }
        }
        return items;
    }

    private void scroll(final Set<WebElement> elements) {
        final List<WebElement> sortedElements = elements
                .stream()
                .sorted(Comparator.comparingInt(a -> a.getLocation().getY()))
                .collect(Collectors.toList());
        Mover.moveToElement(sortedElements.get(0), false);
    }

    @Getter
    public static class Item {
        private final String measuringTypes;
        private final List<String> elements;
        private final String id;
        private final Roles role;
        private final DataVolume dataVolume;

        public Item(final String measuringTypes, final List<String> elements, final String id) {
            this.measuringTypes = measuringTypes;
            this.elements = elements;
            this.id = id;
            final String roleTag = Environment.getScenario().getSourceTagNames()
                                              .stream()
                                              .filter(t -> t.startsWith("@R_"))
                                              .findFirst()
                                              .orElseThrow(() -> new AutotestError("Сценарий не содержит тег роли"));
            role = Roles.getRoleByTagName(roleTag);
            final String dataVolTag = Environment.getScenario().getSourceTagNames()
                                                 .stream()
                                                 .filter(t -> t.startsWith("@LDV_"))
                                                 .map(t -> t.replace("@LDV_", ""))
                                                 .findFirst()
                                                 .orElseThrow(() -> new AutotestError(
                                                         "Сценарий не содержит тег полноты данных"));
            dataVolume = DataVolume.valueOf(dataVolTag);
        }
    }
}
