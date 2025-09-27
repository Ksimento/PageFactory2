package ru.sbt.edu_power.e2e_core.widgets;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс является основой для типа элемента Виджет, который является как
 * самостоятельным элементом, так и хранилищем внутренних элементов
 * Название элемента виджета должно начинаться с большой буквы W, например WUserInfo
 */
public abstract class Widget extends TypifiedElement implements Page {
    private final Page context = this;

    @Override
    public String getTitle() {
        return "Page Widget";
    }

    @Override
    public String getUrl() {
        return "";
    }

    protected Widget(final WebElement wrappedElement) {
        super(wrappedElement);

    }

    public void init() {
        final Field[] fields = this.getClass().getFields();
        for (final Field field : fields) {
            if (field.isAnnotationPresent(FindBy.class)) {
                final Object object;
                if (field.getType().getName().contains("List")) {
                    object = new ArrayList<WebElement>();
                } else {
                    final String xpath = field.getAnnotation(FindBy.class).xpath();
                    final List<WebElement> elements = getWrappedElement().findElements(By.xpath(xpath));
                    if (elements.isEmpty()) {
                        continue;
                    }
                    object = instantiateField(field, elements.get(0));
                    ((TypifiedElement) object).setName(field.getAnnotation(ElementTitle.class).value() +
                                                       " " +
                                                       field.getType().getSimpleName());
                }
                try {
                    field.set(
                            this,
                            object
                    );
                } catch (final IllegalAccessException e) {
                    throw new AutotestError(
                            String.format("Поле \"%s\" не является public", field.getType().getName()),
                            e
                    );
                }
            }
        }
    }

    public WebElement getElementByName(final String nameElement) {
        final Field[] fields = this.getClass().getFields();
        for (final Field field : fields) {
            if (field.isAnnotationPresent(ElementTitle.class)) {
                if (field.getAnnotation(ElementTitle.class).value().equals(nameElement)) {
                    Assert.assertFalse("Список не является веб-элементом", field.getType().getName().contains("List"));
                    try {
                        return (WebElement) field.get(this);
                    } catch (IllegalAccessException e) {
                        throw new AutotestError(
                                String.format("Поле \"%s\" не является public", field.getType().getName()),
                                e
                        );
                    }
                }
            }
        }

        throw new AutotestError(
                String.format("Поле \"%s\" не найдено", nameElement)
        );
    }

    public WebElement getElementByNameOrPath(final String nameOrPath) {
        final Page currentPage = PageContext.getCurrentPage();
        PageContext.setCurrentPage(context);
        final WebElement element;
        try {
            element = FindUtils.getElementByNameOrPath(nameOrPath, true, getWrappedElement());
        } catch (final NoSuchElementException e) {
            throw new AutotestError(e);
        } finally {
            PageContext.setCurrentPage(currentPage);
        }
        return element;
    }

    private TypifiedElement instantiateField(final Field field, final WebElement element) {
        try {
            final Class<?> clazz = Class.forName(field.getType().getName());
            return (TypifiedElement) clazz.getConstructor(WebElement.class)
                                          .newInstance(element);
        } catch (final IllegalAccessException e) {
            throw new AutotestError(String.format("Поле \"%s\" не является public", field.getType().getName()), e);
        } catch (final Exception e) {
            throw new AutotestError("Что-то пошло не так при инициализации поля " + field.getType().getName(), e);
        }
    }
}
