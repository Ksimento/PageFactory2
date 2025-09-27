package ru.sbt.edu_power.e2e_core.survey.blocks;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbt.edu_power.e2e_core.elements.Button;
import ru.sbt.edu_power.e2e_core.elements.survey.SurveySelect;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;

/**
 * Форма конструктора survey - элемент списка вопросов - панель настроек - элемент списка ответов
 */
public class SurveyConfigAnswerListItem extends BlockInitialized {
    public SurveyConfigAnswerListItem(final WebElement webElement) {
        super(webElement);
    }

    @ElementTitle("Тип ячейки")
    @FindBy(xpath = ".//svd-dropdown")
    public SurveySelect cellTypeSurveySelect;

    @ElementTitle("Заголовок")
    @FindBy(xpath = ".//input[@aria-label = 'Заголовок']")
    public TextInput optionTextInput;

    @ElementTitle("Текст")
    @FindBy(xpath = ".//input[@aria-label = 'Текст']")
    public TextInput nameTextInput;

    @ElementTitle("Ссылка на изображение")
    @FindBy(xpath = ".//input[@aria-label = 'Image Link']")
    public TextInput fileLinkTextInput;

    @ElementTitle("Удалить ответ")
    @FindBy(xpath = ".//div[@aria-label = 'Удалить']")
    public Button removeAnswerButton;
}
