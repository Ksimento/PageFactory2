package ru.sbt.edu_power.e2e_core.survey.enums;

/**
 * Перечень кнопок используемых в модальном окне со списком значений
 * Например, список ответов, столбцы или строки
 */
public enum SurveyControlButtons {
//    OK("OK"),
//    CANCEL("Отмена"),
    ADD_NEW("Добавить"),
    DELETE_ALL("Удалить все"),
    RESET("Сброс"),
    REFRESH("Обновить");

    private final String buttonName;

    SurveyControlButtons(final String buttonName) {
        this.buttonName = buttonName;
    }

    public String getButtonName() {
        return buttonName;
    }
}
