package ru.sbt.edu_power.e2e_core.survey.enums;

import ru.sbtqa.tag.qautils.errors.AutotestError;

/**
 * Перечисление всех виджетов конструкторе Survey.
 * enum соответствует классу элемента из ru.sbt.sber_learning.elements.survey.constructor
 * Первый аргумент соответствует названию виджета в описании страницы конструктора CreateTaskPageConstructorSurvey
 * Второй аргумент содержит xpath для автоматической идентификации типа виджета
 */
public enum SurveyWidgetTypes {
    SurveyMatrixSelectOne(
            "Таблица (выбор одного)",
            ".//tbody[contains(@data-testid,'singleChoice')]"
    ),
    SurveyTextInputMultiple(
            "Множественный выбор текста",
            ".//div[contains(@class, 'MultipleTextBox')]//div[contains(@class, '_TextFieldBo')]"
    ),
    SurveyMatrixSelectMultiple(
            "Таблица (множественный выбор)",
            ".//tbody[contains(@data-testid,'manyChoices')]"
    ),
    SurveyTextInput(
            "Поле ввода",
            ".//div[contains(@class, '_TextFieldBo')]"
    ),
    SurveyImageSelect(
            "Выбор изображения",
            ".//img"
    ),
    SurveyImageSelectMultiple(
            "Выбор изображения",
            ".//input[@type = 'checkbox']/following-sibling::div/img"
    ),
    SurveyRadioButton(
            "Группа для выбора одного",
            ".//div[contains(@class, '_ChooseOneFromManyAnswer')]"
    ),
    SurveySelect(
            "Список",
            ".//button[contains(@data-testid, 'dropdown') or contains(@data-testid, 'combobox.trigger')]"
    ),
    SurveySortableWidget(
            "Сортируемые списки",
            ".//div[@class = 'sjs-sortablejs-result']"
    ),
    SurveyCheckBoxGroup(
            "Группа для выбора нескольких",
            ".//div[contains(@class, '_ChooseManyFromManyAnswer')]"
    ),
    SurveyTextBlock(
            "TextBlock",
            ".//div[@class = 'studentAnswer']"
    );

    private final String widgetName;
    private final String detectWidgetXpath;

    SurveyWidgetTypes(final String widgetName, final String detectWidgetXpath) {
        this.widgetName = widgetName;
        this.detectWidgetXpath = detectWidgetXpath;
    }

    public String getWidgetName() {
        return widgetName;
    }

    public String getDetectWidgetXpath() {
        return detectWidgetXpath;
    }

    public static SurveyWidgetTypes getWidgetType(final String widgetName) {
        for (final SurveyWidgetTypes type : SurveyWidgetTypes.values()) {
            if (type.widgetName.equalsIgnoreCase(widgetName)) {
                return type;
            }
        }
        throw new AutotestError(String.format("Тип виджета не определён по значению \"%s\"", widgetName));
    }
}
