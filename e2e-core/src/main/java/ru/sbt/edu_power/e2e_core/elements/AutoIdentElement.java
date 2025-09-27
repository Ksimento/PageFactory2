package ru.sbt.edu_power.e2e_core.elements;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class AutoIdentElement extends TypifiedElement {
    private ParentElementIterator parentIterator;
    private AutoIdentType elementType;

    protected AutoIdentElement(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    protected WebElement getSpecifyElement(final String xpath, final String message) {
        if (null == xpath) {
            return null;
        }
        final WebElement element = getParentIterator().getObject((p) -> {
            final List<WebElement> elements = p.findElements(By.xpath(xpath));
            if (elements.isEmpty()) {
                return null;
            }
            if (elements.size() > 1) {
                throw new AutotestError(message +
                                        "\nНайдено больше одного элемента, требуется уточнить xpath: " +
                                        xpath);
            }
            return elements.get(0);
        });
        if (null == element) {
            final String label = getLabel();
            throw new AutotestError(message + ("".equals(label) ? "" : " для поля " + label));
        }
        return element;
    }

    protected <T> AutoIdentType getElementType(final T[] types) {
        if (null == elementType || parentIterator.isStaled()) {
            final Function<WebElement, T> resolveCheckBoxType = (element) -> Arrays
                    .stream(types)
                    .filter(type -> !element
                            .findElements(By.xpath(((AutoIdentType) type).getIdentificationXpath()))
                            .isEmpty())
                    .findFirst()
                    .orElse(null);
            elementType = (AutoIdentType) getParentIterator().getObject(resolveCheckBoxType);
            if (null == elementType) {
                throw new AutotestError("Тип элемента не определён");
            }
        }
        return elementType;
    }

    protected String getLabel() {
        return "";
    }

    protected WebElement getInput() {
        return getSpecifyElement(
                elementType.getInputXpath(),
                "Не удалось получить элемент для установки значения поля"
        );
    }

    // Метод возвращает объект итератора родительских узлов
    protected ParentElementIterator getParentIterator() {
        if (null == parentIterator || parentIterator.isStaled()) {
            parentIterator = new ParentElementIterator(getWrappedElement());
        }
        return parentIterator;
    }

    public static List<WebElement> findElementInDom(final WebElement element, final String xpathElement) {
        String xpathParent = ".";
        for (int i = 0; i < 3; i++) {
            List<WebElement> elements = element.findElements(By.xpath(xpathParent+xpathElement));
            if (!elements.isEmpty()) {
                return elements;
            }
            xpathParent += "/..";
        }
        return element.findElements(By.xpath(xpathParent + xpathElement));
    }

    public static WebElement findElementInDomNotNull(final WebElement element, final String xpathElement) {
        final List<WebElement> elements = findElementInDom(element, xpathElement);
        if (elements.size() > 1) {
            throw new AutotestError("Найдено больше одного элемента, проверьте уникальность xpath");
        }
        Assert.assertNotNull("Не найден элемент в дереве", elements.get(0));
        return elements.get(0);
    }
}
