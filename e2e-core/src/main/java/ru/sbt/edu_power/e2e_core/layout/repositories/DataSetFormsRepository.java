package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;
import lombok.Setter;

/**
 * Модель данных для измерений типа FORMS
 */
@Getter
@Setter
public class DataSetFormsRepository extends DataSetRepository {
    private final String type;
    private String placeholder;
    private String value;

    public DataSetFormsRepository(
            final String type,
            final String placeholder,
            final String value
    ) {
        this.type = type;
        this.placeholder = placeholder == null ? "" : placeholder;
        this.value = value;
    }
}
