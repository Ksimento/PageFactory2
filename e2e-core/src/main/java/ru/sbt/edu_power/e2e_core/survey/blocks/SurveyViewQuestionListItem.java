package ru.sbt.edu_power.e2e_core.survey.blocks;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbt.edu_power.e2e_core.elements.multiple_type_content.MultipleTypeContent;
import ru.sbt.edu_power.e2e_core.elements.survey.SurveyTypifiedElement;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.yandex.qatools.htmlelements.element.TextBlock;

/**
 * Материалы-задания-предпросмотр-список вопросов
 */
public class SurveyViewQuestionListItem extends BlockInitialized {
    public SurveyViewQuestionListItem(final WebElement webElement) {
        super(webElement);
    }

    @ElementTitle("Вопрос")
    @FindBy(xpath = ".//div[contains(@class, '_Badge')]//p")
    public MultipleTypeContent questionMultipleTypeContent;

    @ElementTitle("Ответ")
    @FindBy(xpath = ".//div[contains(@data-testid, 'custom-constructor-player')]")
    public SurveyTypifiedElement answerSurveyTypifiedElement;

    @ElementTitle("Ответ на задание правильный")
    @FindBy(xpath = ".//p[node()='Ответ принят']")
    public TextBlock correctAnsTextBlock;

    @ElementTitle("Ответ на задание не верный")
    @FindBy(xpath = ".//p[node()='Ответ не принят']")
    public TextBlock incorrectAnsTextBlock;
}
