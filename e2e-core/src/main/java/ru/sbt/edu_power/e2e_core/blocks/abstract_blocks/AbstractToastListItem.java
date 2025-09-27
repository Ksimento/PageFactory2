package ru.sbt.edu_power.e2e_core.blocks.abstract_blocks;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.yandex.qatools.htmlelements.element.TextBlock;

public class AbstractToastListItem extends BlockInitialized {
    public AbstractToastListItem(final WebElement webElement) {
        super(webElement);
    }

    @ElementTitle("Текст уведомления")
    @FindBy(xpath = ".//div[contains(@class, 'Title')]")
    public TextBlock toastTextBlock;

    @ElementTitle("Закрыть всплывающее уведомление")
    @FindBy(xpath = ".//div[contains(@class, 'CloseButton')] | .//*[@data-icon-name = 'ic-clear' or contains(@data-icon-name,'math-x')]")
    public TextBlock closeToastTextBlock;

    @ElementTitle("Тип уведомления - Ошибка")
    @FindBy(xpath = "descendant-or-self::div[contains(@class, 'Toastify__toast--error')]")
    public TextBlock errorToastTextBlock;

    @ElementTitle("Тип уведомления - Успешно")
    @FindBy(xpath = "descendant-or-self::div[contains(@class, 'Toastify__toast--success')]")
    public TextBlock successTextBlock;

    @ElementTitle("Тип уведомления - Предупреждение")
    @FindBy(xpath = "descendant-or-self::div[contains(@class, 'Toastify__toast--warning')]")
    public TextBlock warningTextBlock;
}
