package ru.sbt.edu_power.e2e_core.fields;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.step_defs.E2eCoreStepDefs;
import ru.sbt.edu_power.e2e_core.table_processing.TableActions;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbt.edu_power.e2e_core.widgets.WidgetExtractor;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.blocks.BlockUtils;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.timer.TimerException;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public class FindUtils {

    public static WebElement getElementByNameOrPath(final String nameOrPath) {
        try {
            return getElementByNameOrPath(nameOrPath, false);
        } catch (final TimeoutException | AssertionError e) {
            Allure.addAttachment(
                    "Содержимое DOM",
                    (String) DriverUtils.executeJS("return document.documentElement.innerHTML")
            );
            PageControls.publicateLastBackResponse();
            throw e;
        }
    }

    public static WebElement getElementByNameOrPath(
            final String nameOrPath,
            final boolean fastSearch,
            final WebElement... widget
    ) {
        final WebElement element;
        if (isWidget(nameOrPath)) {
            element = WidgetExtractor.getElement(nameOrPath, fastSearch);
        } else {
            final BlockExtractor block;
            if (widget.length > 0) {
                block = new BlockExtractor(nameOrPath, widget[0]);
            } else {
                block = new BlockExtractor(nameOrPath);
            }
            final BlockUtils.Pattern patternName = block.getLastPattern();
            final int wait = fastSearch ? DriverConstants.ELEMENT_WAIT_5SEC / 5 : DriverConstants.TIME_1SEC * 8;
            switch (patternName) {
                case ELEMENT_IN_BLOCK:
                case WIDGET_IN_BLOCK:
                    element = block.getElementWithWait(wait);
                    break;
                case IDENTIFIED_BLOCK:
                    element = block.getBlockWithWait(wait);
                    break;
                case NON_BLOCK:
                    if (fastSearch) {
                        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                        final List<WebElement> elements;
                        final String xpath = DriverUtils.getXpath(nameOrPath);
                        if (widget.length > 0) {
                            elements = widget[0].findElements(By.xpath(xpath));
                        } else {
                            elements = DriverUtils.findElementsOnPageByXpath(xpath);
                        }
                        Assert.assertFalse(String.format("Элемент \"%s\" не найден", nameOrPath), elements.isEmpty());
                        element = elements.get(0);
                    } else {
                        element = DriverUtils.getElementByTitle(nameOrPath);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(String.format(
                            "Метод не позволяет получать список блоков. \"%s\" является списочным элементом, " +
                            "необходимо добавить параметры поиска блока и, возможно, целевой элемент блока",
                            nameOrPath
                    ));
            }
        }
        Assert.assertNotNull(String.format("Элемент \"%s\" не найден", nameOrPath), element);
        return element;
    }

    public static boolean isWidget(final String path) {
        final String firstElementName;
        if (path.contains("->")) {
            firstElementName = path.split("->")[0];
        } else {
            firstElementName = path;
        }
        final Page page = PageContext.getCurrentPage();
        return Arrays
                .stream(page.getClass().getFields())
                .filter(field -> field.isAnnotationPresent(ElementTitle.class)
                                 &&
                                 firstElementName.equals(field
                                         .getAnnotation(ElementTitle.class)
                                         .value()))
                .findFirst()
                .filter(field -> Widget.class.isAssignableFrom(field.getType()))
                .isPresent();
    }

    public static void checkNotExistsElementTimeout(
            final String elementTitleOrPath,
            final int timeout,
            final boolean isRefresh
    ) {
        final BooleanSupplier checkNotExistsElement = () -> {
            try {
                E2eCoreStepDefs.checkNotExistsElementInBlock(elementTitleOrPath);
            } catch (final RuntimeException | AssertionError ignored) {
                if (isRefresh) {
                    DriverUtils.freeze(5000);
                    PageControls.refreshPage();
                } else {
                    DriverUtils.freeze(1000);
                }
                return false;
            }
            return true;
        };
        Timer.executeTimerThrowable(
                timeout,
                String.format("Элемент \"%s\" не исчез по истечению \"%s\" секунд", elementTitleOrPath, timeout),
                checkNotExistsElement
        );
    }

    public static void waitCheckExistsElement(
            final String elementAndTable,
            final int wait,
            final List<List<String>> data,
            final boolean isElement,
            final boolean isRefresh
    ) {
        final AtomicReference<String> message = new AtomicReference<>();
        final BooleanSupplier waitElement = () -> {
            try {
                if (isElement) {
                    FindUtils.getElementByNameOrPath(elementAndTable, true);
                } else {
                    TableActions.verifyTableRows(elementAndTable, data, true);
                }
            } catch (final AssertionError | TimerException e) {
                message.set(e.getMessage());
                if (isRefresh) {
                    DriverUtils.freeze(5000);
                    PageControls.refreshPage();
                } else {
                    DriverUtils.freeze(1000);
                }
                return false;
            }
            return true;
        };
        if (!Timer.executeTimer(wait, waitElement)) {
            throw new AutotestError(message.get());
        }
    }
}
