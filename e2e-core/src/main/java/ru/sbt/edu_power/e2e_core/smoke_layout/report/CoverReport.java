package ru.sbt.edu_power.e2e_core.smoke_layout.report;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.sbt.edu_power.e2e_core.smoke.Roles;
import ru.sbt.edu_power.e2e_core.smoke_layout.ScreenShotCollection;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.DataVolume;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class CoverReport {
    private final Document document;
    private final Element dataWrapper = new Element("div");
    // Список фильтров по ролям. Для работы фильтра,
    // контейнер с картинкой должен иметь класс role_[ROLE_NAME]
    private final List<Roles> roles = new ArrayList<>();
    private final Element rolesFilter = new Element("div");
    // Список фильтров по объёму наполнения данными. Для работы фильтра,
    // контейнер с картинкой должен содержать класс data-volume_[DATA_VOLUME_NAME]
    private final List<DataVolume> dataVolumes = new ArrayList<>();
    private final Element dataVolumeFilter = new Element("div");
    // хранилище пейджей
    private final Map<Class<? extends Page>, Element> pageSections = new HashMap<>();

    @SneakyThrows
    public CoverReport() {
        final String baseUrl = Objects
                .requireNonNull(this.getClass().getClassLoader().getResource("index.html")).toString();
        document = Jsoup.parse(
                this.getClass().getClassLoader().getResourceAsStream("index.html"),
                StandardCharsets.UTF_8.name(),
                baseUrl
        );

    }

    public void generate() {
        addFilters();
        dataWrapper.attr("class", "data-wrapper");
        document.appendChild(dataWrapper);
        ScreenShotCollection.getINSTANCE()
                            .getPageToScreenShotCollection()
                            .keySet()
                            .forEach(this::generate);
        save();
    }

    private void addFilters() {
        ScreenShotCollection.getINSTANCE()
                            .getPageToScreenShotCollection()
                            .values()
                            .stream()
                            .flatMap(List::stream)
                            .forEach(i -> {
                                if (!roles.contains(i.getRole())) {
                                    roles.add(i.getRole());
                                }
                                if (!dataVolumes.contains(i.getDataVolume())) {
                                    dataVolumes.add(i.getDataVolume());
                                }
                            });

        final Element filterWrapper = new Element("div");
        filterWrapper.attr("class", "filter-wrapper");

        rolesFilter.attr("class", "roles-filter");
        final Element roleFilterName = new Element("span");
        roleFilterName.text("Роли: ");
        rolesFilter.appendChild(roleFilterName);
        roles.sort(Comparator.comparing(Enum::ordinal));
        roles.forEach(role -> {
            final Element checkbox = new Element("input");
            checkbox.attr("type", "checkbox")
                    .attr("name", "role-filter")
                    .attr("id", "role_" + role.getRoleName());
            final Element label = new Element("label");
            label.attr("for", "role_" + role.getRoleName())
                 .text(role.getRoleName());
            rolesFilter.appendChild(label);
            document.body().appendChild(checkbox);
        });

        final Element dataVolumeFilterName = new Element("span");
        dataVolumeFilterName.text("Полнота данных: ");
        dataVolumeFilter.appendChild(dataVolumeFilterName);
        dataVolumeFilter.attr("class", "data-volume-filter");
        dataVolumes.sort(Comparator.comparing(Enum::ordinal));
        dataVolumes.forEach(dv -> {
            final Element checkbox = new Element("input");
            checkbox.attr("type", "radio")
                    .attr("name", "data-volume-filter")
                    .attr("id", "data-volume_" + dv.name());
            if (dv == DataVolume.NORMAL) {
                checkbox.attr("checked", "true");
            }
            final Element label = new Element("label");
            label.attr("for", "data-volume_" + dv.name())
                 .text(dv.name());
            dataVolumeFilter.appendChild(label);
            document.body().appendChild(checkbox);
        });

        final Element sizeCheckbox = new Element("input");
        sizeCheckbox.attr("id", "big-size-toggle")
                    .attr("type", "checkbox");
        final Element toBigSizeToggleLabel = new Element("label");
        toBigSizeToggleLabel.attr("for", "big-size-toggle")
                            .attr("class", "to-big-size-toggle")
                            .text("Увеличить размер");
        final Element toSmallSizeToggleLabel = new Element("label");
        toSmallSizeToggleLabel.attr("for", "big-size-toggle")
                              .attr("class", "to-small-size-toggle")
                              .text("Уменьшить размер");
        filterWrapper.appendChild(rolesFilter)
                     .appendChild(dataVolumeFilter)
                     .appendChild(toBigSizeToggleLabel)
                     .appendChild(toSmallSizeToggleLabel);
        document.body()
                .appendChild(sizeCheckbox)
                .appendChild(filterWrapper);

    }

    private void generate(final Class<? extends Page> page) {
        ScreenShotCollection.getINSTANCE().getPageToScreenShotCollection().get(page).forEach(i -> generate(page, i));
    }

    private void generate(final Class<? extends Page> page, final ScreenShotCollection.Item item) {
        final List<String> itemClasses = new ArrayList<>();
        itemClasses.add("role_" + item.getRole().getRoleName());
        itemClasses.add("data-volume_" + item.getDataVolume().name());
        // контейнер для всех элементов страницы
        final Element itemsSection = getPageSection(page, item.getRole());
        // контейнер для элемента
        final Element itemContainer = new Element("div");
        itemContainer.attr("class", "item-container " + String.join(" ", itemClasses));
        final Element itemRadio = new Element("input");
        itemRadio.attr("id", item.getId())
                 .attr("type", "radio")
                 .attr("class", "item-sections")
                 .attr("name", "item-sections_" +
                               item.getRole().getRoleName() + "_" +
                               page.getSimpleName());
        final Element itemLabel = new Element("label");
        itemLabel.attr("for", item.getId())
                 .attr("class", "item-sections");
        final Element labelText = new Element("h3");
        labelText.text(item.getMeasuringTypes() + " / " + String.join(", ", item.getElements()));
        final Element itemImg = new Element("img");
        itemImg.attr("src", "images/" + item.getId() + ".png");
        itemLabel.appendChild(labelText)
                 .appendChild(itemImg);
        itemContainer.appendChild(itemLabel);
        itemsSection.appendChild(itemRadio)
                    .appendChild(itemContainer);

    }

    private Element getPageSection(final Class<? extends Page> page, final Roles role) {
        if (!pageSections.containsKey(page)) {
            final Element pageLabel = new Element("label");
            pageLabel.attr("for", page.getSimpleName())
                     .attr("class", "pages")
                     .text(page.getAnnotation(PageEntry.class).title());
            final Element pageTitle = new Element("h2");
            pageTitle.appendChild(pageLabel);
            final Element checkbox = new Element("input");
            checkbox.attr("id", page.getSimpleName())
                    .attr("type", "checkbox")
                    .attr("class", "page-sections")
                    .attr("name", "page-sections_" + role.getRoleName());
            final Element itemContainer = new Element("div");
            itemContainer.attr("class", "items-container");
            final Element pageWrapper = new Element("div");
            pageWrapper.attr("class", "page-wrapper");

            pageWrapper.appendChild(pageTitle)
                       .appendChild(checkbox)
                       .appendChild(itemContainer);
            dataWrapper.appendChild(pageWrapper);
            pageSections.put(page, itemContainer);
        }
        if (!pageSections.get(page).parent().attr("class").contains(role.getRoleName())) {
            pageSections
                    .get(page)
                    .parent()
                    .attr("class", pageSections.get(page).parent().attr("class") + " role_" + role.getRoleName());
        }
        return pageSections.get(page);
    }

    private void save() {
        final Path path = Paths.get(ScreenShotCollection.REPORT_PATH, "index.html");
        try {
            Files.write(path, document.toString().getBytes(StandardCharsets.UTF_8));
            try (final InputStream is = Objects.requireNonNull(this
                    .getClass()
                    .getClassLoader()
                    .getResourceAsStream("style.css"))
            ) {
                final Path cssPath = Paths.get(ScreenShotCollection.REPORT_PATH, "style.css");
                FileUtils.copyInputStreamToFile(is, cssPath.toFile());
            }
        } catch (final IOException e) {
            throw new AutotestError("Не удалось записать документ на диск", e);
        }

    }
}
