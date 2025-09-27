package ru.sbt.edu_power.e2e_core.blocks;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Родительский класс для блочных элементов. Реализует инициализацию блока и всех его элементов при создании нового
 */
@Slf4j
public abstract class BlockInitialized extends HtmlElement {
    protected BlockInitialized(final WebElement webElement) {
        super.setName("Блок");
        super.setWrappedElement(webElement);
        final Field[] fields = this.getClass().getFields();
        for (final Field field : fields) {
            if (field.isAnnotationPresent(FindBy.class)) {
                final Object object;
                if (field.getType().getName().contains("List")) {
                    object = new ArrayList<WebElement>();
                } else {
                    final String xpath = field.getAnnotation(FindBy.class).xpath();
                    final List<WebElement> elements = webElement.findElements(By.xpath(xpath));
                    if (elements.isEmpty()) {
                        continue;
                    }
                    object = instantiateField(field, elements.get(0));
                    ((TypifiedElement) object).setName(field.getAnnotation(ElementTitle.class).value() + " " + field.getType().getSimpleName());
                }
                try {
                    field.set(
                            this,
                            object
                    );
                } catch (final IllegalAccessException e) {
                    throw new BlockExtractorException(String.format("Поле \"%s\" не является public", field.getType().getName()), e);
                }
            }
        }
    }

    private TypifiedElement instantiateField(final Field field, final WebElement element) {
        try {
            final Class<?> clazz = Class.forName(field.getType().getName());
            return (TypifiedElement) clazz.getConstructor(WebElement.class)
                    .newInstance(element);
        } catch (final IllegalAccessException e) {
            throw new BlockExtractorException(String.format("Поле \"%s\" не является public", field.getType().getName()), e);
        } catch (final Exception e) {
            throw new BlockExtractorException("Что-то пошло не так при инициализации поля " + field.getType().getName(), e);
        }
    }

    /**
     * Метод возвращает инициализированный веб-элемент из блока по его названию ElementTitle
     *
     * @param path Название элемента в блоке
     * @return инициализированный веб-элемент
     */
    public WebElement getElementByName(final String path) {
        try {
            if (path.contains("->")) {
                final String[] parts = path.split("->");
                final Field field = getFieldByName(parts[0]);
                if (Widget.class.isAssignableFrom(field.getType())) {
                    final Widget widget = (Widget) field.get(this);
                    widget.init();
                    return widget.getElementByNameOrPath(String.join("->", Arrays.copyOfRange(parts, 1, parts.length)));
                } else {
                    throw new AutotestError(String.format("Поле %s должно быть виджетом", parts[0]));
                }
            }
            final Field field = getFieldByName(path);
            if (Widget.class.isAssignableFrom(field.getType())) {
                final Widget widget = (Widget) field.get(this);
                widget.init();
                return widget;
            }
            return (WebElement) field.get(this);
        } catch (final IllegalAccessException e) {
            throw new BlockExtractorException(String.format("Элемент \"%s\" объявлен как приватный", path), e);
        } catch (final ClassCastException e) {
            throw new BlockExtractorException(String.format("Элемент \"%s\" не является веб-элементом", path), e);
        }
    }

    private Field getFieldByName(final String name) {
        Assert.assertNotEquals("Вы указали блок, но не указали имя поля с которым нужно произвести действие", name, "");
        for (final Field field : this.getClass().getFields()) {
            if (field.isAnnotationPresent(ElementTitle.class)
                    && field.getAnnotation(ElementTitle.class).value().equals(name)) {
                return field;
            }
        }
        throw new NoElementFoundInBlockContext(String.format("Поле \"%s\" не объявлено в классе \"%s\"", name, this.getClass()));
    }
}