package ru.sbt.edu_power.e2e_core.survey.enums;

import lombok.Getter;
import ru.sbtqa.tag.qautils.errors.AutotestError;

/**
 * Список заполняемых данных в виджетах
 * Параметр соответствует названию элемента в описании страницы конструктора CreateTaskPageConstructorSurvey
 * Заголовок и Multi Select - заполняемые поля в правой панели конфигурации виджета
 * Остальные - кнопки для открытия модального окна с наборами полей
 */
@Getter
public enum SurveyDataTypes {
    QUESTION("Заголовок", "Общее"),
    MULTI_SELECT("Multi Select","Общее"),
    ANSWER_LIST("Ответы", "Выбор"),
    TABLE_COLUMNS("Столбцы", "Столбцы"),
    TABLE_ROWS("Строки", "Строки"),
    OBJECTS("Объекты", "Объекты"),
    CORRECT_ANSWER("Правильный ответ", "Данные"),
    DEFAULT_VALUE("Значение по умолчанию", "Данные");

    private final String fieldName;
    private final String tabName;

    SurveyDataTypes(final String fieldName, final String tabName) {
        this.fieldName = fieldName;
        this.tabName = tabName;
    }

    public static SurveyDataTypes getDataType(final String type) {
        for (final SurveyDataTypes dataType : SurveyDataTypes.values()) {
            if (type.equalsIgnoreCase(dataType.getFieldName())) {
                return dataType;
            }
        }
        throw new AutotestError(String.format("Тип данных не определён по значению \"%s\"", type));
    }
}
