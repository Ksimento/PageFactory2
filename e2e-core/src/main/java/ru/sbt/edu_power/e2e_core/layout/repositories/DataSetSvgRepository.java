package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;

/**
 * Модель данных для измерений типа SVG
 */
@Getter
public class DataSetSvgRepository extends DataSetRepository {
    private final String svgPath_1;
    private final String svgPath_2;
    private final String svgPath_3;
    private final String fillColor;

    public DataSetSvgRepository(
            final String svgPath_1,
            final String svgPath_2,
            final String svgPath_3,
            final String fillColor
    ) {
        this.svgPath_1 = svgPath_1;
        this.svgPath_2 = svgPath_2;
        this.svgPath_3 = svgPath_3;
        this.fillColor = fillColor;
    }
}
