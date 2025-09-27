package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;
import lombok.Setter;

/**
 * Модель данных для измерений типа TEXT
 */
@Getter
@Setter
public class DataSetTextRepository extends DataSetRepository {
    private final String color;
    private final String size;
    private final String family;
    private final String weight;
    private String content;
    private final String tag;

    public DataSetTextRepository(
            final String color,
            final String size,
            final String family,
            final String weight,
            final String content,
            final String tag
    ) {
        this.color = color;
        this.size = size;
        this.family = family.replaceAll("\"", "'");
        this.weight = weight;
        this.content = content
                .replaceAll("\"", "'")
                .replaceAll("\n", " ");
        this.tag = tag;
    }
}