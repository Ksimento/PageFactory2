package ru.sbt.edu_power.e2e_core.survey;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.elements.survey.ElementPartValidatable;
import ru.sbt.edu_power.e2e_core.elements.survey.SurveySelect;
import ru.sbt.edu_power.e2e_core.survey.blocks.SurveyConstructorQuestionListItem;
import ru.sbt.edu_power.e2e_core.survey.blocks.SurveyViewQuestionListItem;
import ru.sbt.edu_power.e2e_core.survey.enums.LoadControlMethod;
import ru.sbt.edu_power.e2e_core.survey.enums.SurveyAnswerFieldList;
import ru.sbt.edu_power.e2e_core.survey.enums.SurveyWidgetTypes;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.blocks.PathBuilder;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.survey.enums.SurveyDataTypes;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.List;
import java.util.Map;

@Slf4j
public class SurveyStepActions extends SurveyStepActionsUtils {
    private final String CONSTRUCTOR_WIDGET_LIST_ITEM = "Список вопросов";
    private final String ANSWER_LIST = "Список вопросов survey";
    private final String SURVEY_QUESTION_LIST_ITEM_TITLE = "Заголовок";
    private final String WIDGET_CONFIGURATION_PANEL_XPATH = "//div[contains(@class, 'svd_properties')]";

    private SurveyWidgetTypes typeOfActiveWidget;

    /**
     * Метод выполняет добавление и конфигурирование виджета Survey в конструкторе
     *
     * @param widgetName название виджета
     * @param data       данные для заполнения полей виджета
     */
    public void addAndConfigureWidget(final String widgetName, final Map<String, String> data) {
        addWidget(widgetName);
        configureActiveWidget(data);
    }

    /**
     * Метод выполняет добавление виджета в список вопросов по его названию
     *
     * @param widgetName название виджета
     */
    public void addWidget(final String widgetName) {
        final WebElement widgetButton = DriverUtils.getElementByTitle(
                SurveyWidgetTypes.getWidgetType(widgetName).getWidgetName()
        );
        SurveyActions.clickWithChangeControl(
                widgetName,
                widgetButton,
                LoadControlMethod.BY_BLOCK,
                CONSTRUCTOR_WIDGET_LIST_ITEM
        );
        typeOfActiveWidget = SurveyWidgetTypes.getWidgetType(widgetName);
    }

    /**
     * Метод выполняет заполнение уже добавленного в список вопросов виджета
     *
     * @param data             данные для заполнения полей виджета
     * @param numberOrQuestion номер вопроса или вопрос (поддерживается маска из звёздочек)
     */
    public void configureWidget(final Map<String, String> data, final String numberOrQuestion) {
        selectWidget(numberOrQuestion);
        configureActiveWidget(data);
    }

    /**
     * Метод выполняет поиск виджета в списке вопросов по номеру или тексту вопроса
     * и делает его активным (кликает по нему)
     *
     * @param numberOrQuestionDraft номер или текст вопроса
     */
    public void selectWidget(final String numberOrQuestionDraft) {
        final String numberOrQuestion = DataProcessing.decodeValue(numberOrQuestionDraft);
        final Integer number = DriverUtils.getNumber(numberOrQuestion);
        final PathBuilder path = new PathBuilder()
                .setBlock(CONSTRUCTOR_WIDGET_LIST_ITEM);
        if (null != number) {
            path.setBlockNumber(number);
        } else {
            path.setBlockArgument(SURVEY_QUESTION_LIST_ITEM_TITLE, numberOrQuestion);
        }
        final SurveyConstructorQuestionListItem block = new BlockExtractor(path.build()).getBlock();
        SurveyActions.clickWithChangeControl(
                numberOrQuestion,
                block,
                LoadControlMethod.BY_XPATH,
                WIDGET_CONFIGURATION_PANEL_XPATH
        );
        final String widgetName;
        if ("span".equals(block.currentWidgetTypeWebElement.getTagName())) {
            widgetName = block.currentWidgetTypeWebElement.getText();
        } else {
            widgetName = (String) DriverUtils.executeJS(
                    "return arguments[0].innerHTML",
                    new SurveySelect(block.currentWidgetTypeWebElement).getFirstSelectedOption()
            );
        }
        typeOfActiveWidget = SurveyWidgetTypes.getWidgetType(widgetName);
    }

    /**
     * Метод выполняет заполнение полей активного в данный момент виджета
     *
     * @param data данные для заполнения
     */
    public void configureActiveWidget(final Map<String, String> data) {
        final Map<SurveyDataTypes, List<Map<SurveyAnswerFieldList, String>>> preparedData = prepareData(
                data,
                typeOfActiveWidget
        );
        preparedData.forEach((k, v) -> {
            if (k == SurveyDataTypes.QUESTION) {
                fillQuestion(v.get(0).get(SurveyAnswerFieldList.TEXT));
            } else if (k == SurveyDataTypes.MULTI_SELECT) {
                fillMultiSelect(v.get(0).get(SurveyAnswerFieldList.TEXT));
            } else if (k == SurveyDataTypes.CORRECT_ANSWER) {
                fillCorrectAnswer(v.get(0).get(SurveyAnswerFieldList.TEXT));
            } else {
                openFieldsTab(k);
                deleteAllFields(k);
                v.forEach(answer -> {
                    addFieldsBlock(k);
                    fillAnswer(k, answer);
                });
                closeFieldTab(k);
            }
        });
    }

    /**
     * Метод выполняет проверку заполнения полей активного в данный момент виджета
     *
     * @param data данные для заполнения
     */
    public void verifyActiveWidget(final Map<String, String> data) {
        final Map<SurveyDataTypes, List<Map<SurveyAnswerFieldList, String>>> preparedData = prepareData(
                data,
                typeOfActiveWidget
        );
        preparedData.forEach((k, v) -> {
            if (k == SurveyDataTypes.QUESTION) {
                verifyQuestion(v.get(0).get(SurveyAnswerFieldList.TEXT));
            } else if (k == SurveyDataTypes.MULTI_SELECT) {
                verifyMultiSelect(v.get(0).get(SurveyAnswerFieldList.TEXT));
            } else if (k == SurveyDataTypes.CORRECT_ANSWER) {
                verifyCorrectAnswer(v.get(0).get(SurveyAnswerFieldList.TEXT));
            } else {
                openFieldsTab(k);
                verifyAnswers(k, v);
            }
        });
    }

    public void fillAnswerListByStudent(final Map<String, String> data) {
        for (final String key : data.keySet()) {
            final String question = DataProcessing.decodeValue(key);
            final SurveyViewQuestionListItem block = getSurveyQuestionBlock("Список вопросов survey", question);
            final Fillable field = createTypifiedSurveyInputElement(
                    question,
                    block.answerSurveyTypifiedElement
            );
            try {
                field.fillField(data.get(key), true);
            } catch (final FieldFillingException e) {
                throw new AutotestError("Не удалось заполнить поле " + key, e);
            }
        }
    }

    public void checkAnswersByBorderColor(final Map<String, String> data) {
        final ErrorCollector errorCollector = new ErrorCollector();
        for (final String key : data.keySet()) {
            final String question = DataProcessing.decodeValue(key);
            final SurveyViewQuestionListItem block = getSurveyQuestionBlock(ANSWER_LIST, question);
            final boolean expected = DriverUtils.getBoolean(data.get(key));
            if (expected) {
                errorCollector.assertTrue(
                        String.format("Ожидается, что ответ на вопрос \"%s\" будет верным, но он неверный", question),
                        DriverUtils.isElementDisplayed(block.correctAnsTextBlock)
                );
            } else {
                errorCollector.assertTrue(
                        String.format("Ожидается, что ответ на вопрос \"%s\" будет неверным, но он верный", question),
                        DriverUtils.isElementDisplayed(block.incorrectAnsTextBlock)
                );
            }
        }
        errorCollector.assertAll();
    }

    public void partialElementsCheck(final String question, final Map<String, String> data) {
        final ErrorCollector errorCollector = new ErrorCollector();
        for (final String elementName : data.keySet()) {
            final String questionParam = DataProcessing.decodeValue(question);
            final SurveyViewQuestionListItem block = getSurveyQuestionBlock(ANSWER_LIST, questionParam);
            final ElementPartValidatable elementPartValidatable = createTypifiedSurveyInputElement(
                    questionParam,
                    block
            );
            final boolean result = elementPartValidatable.partialValidate(elementName, data.get(elementName));
            final String actual = elementPartValidatable.getPartContent(elementName);
            errorCollector.assertTrue(
                    String.format(
                            "Ожидаемое значение \"%s\" не соответствует актуальному \"%s\"",
                            data.get(elementName), actual
                    ),
                    result
            );
        }
        errorCollector.assertAll();
    }
}
