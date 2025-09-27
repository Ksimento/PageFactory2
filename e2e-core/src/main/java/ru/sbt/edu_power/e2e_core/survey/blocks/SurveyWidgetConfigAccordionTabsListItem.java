package ru.sbt.edu_power.e2e_core.survey.blocks;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbt.edu_power.e2e_core.elements.Button;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.yandex.qatools.htmlelements.element.TextBlock;

import java.util.List;

public class SurveyWidgetConfigAccordionTabsListItem extends BlockInitialized {
    public SurveyWidgetConfigAccordionTabsListItem(final WebElement webElement) {
        super(webElement);
    }

    @ElementTitle("Название")
    @FindBy(xpath = ".")
    public TextBlock tabNameTextBlock;

    @ElementTitle("Контент под табом")
    @FindBy(xpath = "following-sibling::div/div")
    public TextBlock tabContentTextBlock;

    @ElementTitle("Удалить все")
    @FindBy(xpath = "following-sibling::div//input[@value = 'Удалить все']")
    public Button removeAllButton;

    @ElementTitle("Добавить")
    @FindBy(xpath = "following-sibling::div//input[@value = 'Добавить']")
    public Button addItemButton;

    @ElementTitle("Список ответов")
    @FindBy(xpath = "following-sibling::div//tbody/tr")
    public List<SurveyConfigAnswerListItem> surveyConfigAnswerList;
}
