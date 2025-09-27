package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Модель данных для измерений типа BEFORE_AFTER
 */
@Getter
public class DataSetBeforeAfterRepository extends DataSetRepository {
    private final String content;
    private final String color;
    private final String background;


    public DataSetBeforeAfterRepository(
            final String content,
            final String color,
            final String background
    ) {
        this.content = content;
        this.color = color;
        final String purifiedBackground = Stream.of(background.split("/"))
                .filter(t -> !(t.contains("pcbltools.ru")))
                .collect(Collectors.joining("/"));
        this.background = getMinifyedString(purifiedBackground);
    }
}
