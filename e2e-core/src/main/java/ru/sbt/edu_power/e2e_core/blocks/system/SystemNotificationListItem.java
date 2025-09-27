package ru.sbt.edu_power.e2e_core.blocks.system;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.yandex.qatools.htmlelements.element.TextBlock;

public class SystemNotificationListItem extends BlockInitialized {
    public SystemNotificationListItem(final WebElement webElement) {
        super(webElement);
    }

    @ElementTitle("Наименование")
    @FindBy(xpath = ".//span[contains(@class, '_AdvertHeader')]")
    public TextBlock notificationTitleTextBlock;

    @ElementTitle("Сообщение")
    @FindBy(xpath = ".//span[contains(@class, '_Description')]")
    public TextBlock notificationDescriptionTextBlock;

    @ElementTitle("Закрыть")
    @FindBy(xpath = ".//*[name() = 'svg']")
    public TextBlock notificationCloseTextBlock;
}
