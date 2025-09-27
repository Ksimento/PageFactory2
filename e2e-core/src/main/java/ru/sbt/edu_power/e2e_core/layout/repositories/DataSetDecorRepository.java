package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Модель данных для измерений типа DECOR
 */
@Getter
public class DataSetDecorRepository extends DataSetRepository {
    private final String border;
    private final String borderRadius;
    private final String boxShadow;
    private final String background;


    public DataSetDecorRepository(
            final String border,
            final String borderRadius,
            final String boxShadow,
            final String background
    ) {
        this.border = border;
        this.borderRadius = borderRadius;
        this.boxShadow = boxShadow;
        final String purifiedBackground = Stream.of(background.split("/"))
                .filter(t -> !(t.contains("pcbltools.ru")))
                .collect(Collectors.joining("/"));
        this.background = getMinifyedString(purifiedBackground);
    }
}
