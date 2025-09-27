package ru.sbt.edu_power.e2e_core.survey.enums;

import ru.sbtqa.tag.qautils.errors.AutotestError;

/**
 * Список доступных вариантов представления выбора данных в таблице с множественным выбором
 */
public enum  SurveyAnswerFieldRenderVariant {
    Default("По умолчанию"),
    dropdown("Список"),
    checkbox("Чекбокс"),
    radiogroup("Группа для выбора одного"),
    text("Поле ввода"),
    rating("Рейтинг");

    private final String name;

    SurveyAnswerFieldRenderVariant(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SurveyAnswerFieldRenderVariant getFieldRenderVariant(final String fieldVariant) {
        for (final SurveyAnswerFieldRenderVariant fieldRenderVariant : SurveyAnswerFieldRenderVariant.values()) {
            if (fieldRenderVariant.name().equalsIgnoreCase(fieldVariant)
                || fieldRenderVariant.getName().equalsIgnoreCase(fieldVariant)) {
                return fieldRenderVariant;
            }
        }
        throw new AutotestError(String.format(
                "Тип варианта рендера поля не определён по значению \"%s\"",
                fieldVariant
        ));
    }
}
