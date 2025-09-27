package ru.sbt.edu_power.e2e_core.survey.enums;

/**
 * Перечисление всех возможных полей в одном элементе списка ответов (или строк или столбцов)
 */
public enum SurveyAnswerFieldList {
//    REQUIRED("Обязательное?"),
    CELL_TYPE("Тип ячейки"),
    OPTION("Заголовок"),
    TEXT("Текст"),
    IMAGE_LINK("Ссылка на изображение"),
    REMOVE_ANSWER("Удалить ответ");

    private final String fieldName;

    SurveyAnswerFieldList(final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
