package ru.sbt.edu_power.e2e_core.layout.enums;

public enum ElementsColor {
    RED("rgba(255,0,0,0.5)"),
    GREEN("rgba(0,255,0,0.5)"),
    ORANGE("rgba(255,128,0,0.5)"),
    BLUE("rgba(0,0,255,0.5)"),
    VIOLET("rgba(238,130,238,0.5)"),
    MAGENTA("rgba(255,0,255,0.5)"),
    BROWN("rgba(165,42,42,0.5)"),
    NAVY("rgba(0,0,128,0.5)"),
    DEFAULT("");

    private final String color;

    ElementsColor(final String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
