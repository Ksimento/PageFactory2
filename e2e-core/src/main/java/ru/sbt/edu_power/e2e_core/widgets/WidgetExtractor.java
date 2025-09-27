package ru.sbt.edu_power.e2e_core.widgets;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

// Класс реализует функциональность получения элемента виджета по его названию или получения элемента из виджета
public class WidgetExtractor {

    public static WebElement getElement(final String path) {
        return getElement(path, false);
    }

    public static WebElement getElement(final String path, final boolean fastSearch) {
        if (path.contains("->")) {
            final Widget widget = getWidget(path.split("->")[0], fastSearch);
            return widget.getElementByNameOrPath(path.split("->", 2)[1]);
        } else {
            return getWidget(path, fastSearch);
        }
    }

    public static <T extends Widget> T getWidget(final String widgetName) {
        return getWidget(widgetName, false);
    }

    public static <T extends Widget> T getWidget(final String widgetName, final boolean fastSearch) {
        final String xpath = DriverUtils.getXpath(widgetName);
        final List<WebElement> elements = new ArrayList<>();
        final BooleanSupplier waitWhenWidgetBeFound = () -> {
            elements.addAll(DriverUtils.findElementsOnPageByXpath(xpath));
            return !elements.isEmpty();
        };
        final int wait = fastSearch ? DriverConstants.ELEMENT_WAIT_5SEC : DriverConstants.TIMEOUT;

        final String message = String.format("Элемент \"%s\" не найден по xpath \"%s\"", widgetName, xpath);
        Timer.executeTimerThrowable(wait, message, waitWhenWidgetBeFound);
        return instantiateWidget(widgetName, elements.get(0));
    }

    public static <T extends Widget> T instantiateWidget(final String widgetName, final WebElement element) {
        final Page page = PageContext.getCurrentPage();
        for (final Field field : page.getClass().getFields()) {
            if (field.isAnnotationPresent(ElementTitle.class)
                && widgetName.equals(field.getAnnotation(ElementTitle.class).value())) {
                return instantiateWidget(field, element);
            }
        }
        throw new AutotestError(String.format("На страницы \"%s\" нет элемента \"%s\"", page.getTitle(), widgetName));
    }


    @SuppressWarnings("unchecked")
    public static <T extends Widget> T instantiateWidget(final Field field, final WebElement element) {
        try {
            return (T) field.getType().getConstructor(WebElement.class).newInstance(element);
        } catch (final InstantiationException e) {
            throw new AutotestError(e);
        } catch (final IllegalAccessException e) {
            throw new AutotestError(
                    "Необходимо объявить класс как public " + field.getDeclaringClass().getName(),
                    e
            );
        } catch (final InvocationTargetException e) {
            throw new AutotestError(
                    "Не возможно создать экземпляр класса " + field.getType().getName(),
                    e
            );
        } catch (final NoSuchMethodException e) {
            throw new AutotestError(
                    "Не объявлен конструктор для класса " + field.getType().getName(),
                    e
            );
        }
    }

}
