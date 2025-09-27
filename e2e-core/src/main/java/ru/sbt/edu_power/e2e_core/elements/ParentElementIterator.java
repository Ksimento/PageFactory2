package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.function.Function;

/**
 * Класс реализует возможность последовательного перехода к родительскому веб-элементу
 * до тех пор, пока не будет найден контейнер, охватывающий все составляющие вебэлемента
 */
public class ParentElementIterator {
    // Максимально допустимый уровень подъёма по DOM дереву
    private final int PARENT_DEPTH;
    // Текущий уровень подъёма относительно wrappedElement
    private int currentParentDepth;
    // Веб-элемент контейнера соответствующий текущему уровню подъёма
    private WebElement parent;

    public ParentElementIterator(final int parentDepth, final WebElement element) {
        PARENT_DEPTH = parentDepth;
        parent = element;
    }

    public ParentElementIterator(final WebElement element) {
        PARENT_DEPTH = 3;
        parent = element;
    }

    // Метод проверяет возможность перехода выше
    public boolean hasNext() {
        return currentParentDepth < PARENT_DEPTH;
    }

    // Метод выполняет переход к родительскому узлу
    public void next() {
        if (hasNext()) {
            parent = parent.findElement(By.xpath("parent::*"));
            currentParentDepth++;
        } else {
            throw new AutotestError("Достигнут максимальный уровень подъёма");
        }
    }

    public WebElement getCurrentParent() {
        return parent;
    }

    // Метод проверяет доступность текущего родительского узла
    public boolean isStaled() {
        try {
            parent.isDisplayed();
        } catch (final StaleElementReferenceException e) {
            return true;
        }
        return false;
    }

    /**
     * Метод выполняет пошаговый подъём по дереву DOM до тех пор, пока
     * переданная в метод функция не вернёт объект или пока не будет достигнут
     * предел подъёма
     *
     * @param function функция должна возвращать любой объект, в качестве аргумента
     *                 должна принимать текущий родительский узел
     * @param <T>
     * @return
     */
    public <T> T getObject(final Function<WebElement, T> function) {
        while (true) {
            final T object = function.apply(parent);
            if (null != object) {
                return object;
            }
            if (hasNext()) {
                next();
            } else {
                return null;
            }
        }
    }
}
