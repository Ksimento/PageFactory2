package ru.sbt.sber_learning.blocks.main;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbt.edu_power.e2e_core.elements.image.BackgroundImage;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;

public class ConfiguratorProfileListItem extends BlockInitialized {
    public ConfiguratorProfileListItem(WebElement webElement) {
        super(webElement);
    }

    @ElementTitle("Фото")
    @FindBy(xpath = ".//div[@data-testid = 'TeacherProfileLinkAvatar.Avatar']")
    public BackgroundImage userImage;
}
