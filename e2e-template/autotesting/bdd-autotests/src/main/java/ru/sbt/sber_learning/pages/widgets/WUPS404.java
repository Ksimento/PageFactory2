package ru.sbt.sber_learning.pages.widgets;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.widgets.IsUps404Widget;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.yandex.qatools.htmlelements.element.TextBlock;

public class WUPS404 extends Widget implements IsUps404Widget {
    public WUPS404(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @ElementTitle("Заголовок")
    @FindBy(xpath = ".//h1")
    public TextBlock headerTextBlock;

    @ElementTitle("Сообщение")
    @FindBy(xpath = ".//h2")
    public TextBlock messageTextBlock;

    @ElementTitle("Причина")
    @FindBy(xpath = ".//p[last()]")
    public TextBlock reasonTextBlock;

    @Override
    public String getHeaderText() {
        return headerTextBlock.getText();
    }

    @Override
    public String getMessageText() {
        return messageTextBlock.getText();
    }

    @Override
    public String getReasonText() {
        return reasonTextBlock.getText();
    }
}
