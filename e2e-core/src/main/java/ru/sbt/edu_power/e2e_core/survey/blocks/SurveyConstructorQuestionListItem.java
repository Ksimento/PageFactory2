package ru.sbt.edu_power.e2e_core.survey.blocks;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbt.edu_power.e2e_core.elements.multiple_type_content.MultipleTypeContent;
import ru.sbt.edu_power.e2e_core.elements.dropdown.Select;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.yandex.qatools.htmlelements.element.TextBlock;

/**
 * Форма конструктора Survey - область контента - список вопросов - элемент списка
 */
public class SurveyConstructorQuestionListItem extends BlockInitialized {
    public SurveyConstructorQuestionListItem(final WebElement webElement) {
        super(webElement);
    }

    @ElementTitle("Номер вопроса")
    @FindBy(xpath = ".//span[contains(@class, 'sv_q_num')]")
    public TextBlock questNumberTextBlock;

    @ElementTitle("Заголовок")
    @FindBy(xpath = ".//div[contains(@class, '')]")
    public MultipleTypeContent titleMultipleTypeContent;

    @ElementTitle("Правка")
    @FindBy(xpath = ".//span[@title = 'Правка']")
    public TextBlock editTextBlock;

    @ElementTitle("Удалить вопрос")
    @FindBy(xpath = ".//span[@title = 'Удалить вопрос']")
    public TextBlock deleteTextBlock;

    @ElementTitle("Отображать/скрыть заголовок")
    @FindBy(xpath = ".//span[@title = 'Отображать/скрыть заголовок']")
    public TextBlock toggleViewTextBlock;

    @ElementTitle("Обязательность")
    @FindBy(xpath = ".//span[@title = 'Обязательность?']")
    public TextBlock obligatoryTextBlock;

    @ElementTitle("Скопировать")
    @FindBy(xpath = ".//span[@title = 'Скопировать']")
    public TextBlock copyTextBlock;

    @ElementTitle("Добавить в панель")
    @FindBy(xpath = ".//span[@title = 'Добавить в панель']")
    public TextBlock addToPanelTextBlock;

    @ElementTitle("Конвертировать в")
    @FindBy(xpath = ".//select[@title = 'Конвертировать в']")
    public Select convertToAnotherTypeSelect;

    @ElementTitle("Текущий тип виджета")
    @FindBy(xpath = ".//*[contains(@class, 'svda_current_type')]")
    public TextBlock currentWidgetTypeWebElement;
}
