package ru.sbt.edu_power.e2e_core.layout.enums;

import lombok.Getter;

/**
 * Список доступных разрешений для выполнения тестов на вёрстку
 */
@Getter
public enum DimensionEnum {
    FULL_HD("1920x1080", 1902, 957, false, 1),
    DESKTOP("1680x900", 1672, 777, false, 1),
    DEFAULT(DESKTOP.dimensionName, DESKTOP.bodyWidth, DESKTOP.bodyHeight, false, 1),
    TABLET_LANDSCAPE("1024x768", 1024, 645,true, 1),
    TABLET_PORTRAIT("768x1024", 768, 901, true, 0.85),
    MOBILE_PORTRAIT("320x568", 320, 445, true, 1.6);

    private final String dimensionName;

    private final int bodyWidth;

    private final int bodyHeight;

    private final boolean isMobileDimension;

    private final double scaleFactor;

    DimensionEnum(
            final String dimensionName,
            final int bodyWidth,
            final int bodyHeight,
            final boolean isMobileDimension,
            final double scaleFactor
    ) {
        this.dimensionName = dimensionName;
        this.bodyWidth = bodyWidth;
        this.bodyHeight = bodyHeight;
        this.isMobileDimension = isMobileDimension;
        this.scaleFactor = scaleFactor;
    }
}
