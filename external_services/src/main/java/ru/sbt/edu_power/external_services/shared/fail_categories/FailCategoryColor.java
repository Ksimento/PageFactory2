package ru.sbt.edu_power.external_services.shared.fail_categories;

import lombok.Getter;

@Getter
public enum FailCategoryColor {
    BLUE("#375fed"),
    BROWN("#964B00"),
    YELLOW("#FFFF00"),
    GREEN("#008000"),
    PURPLE("#8B00FF"),
    ORANGE("#FFA500"),
    GOLD("#FFD700"),
    RED("#FF0000");

    private final String color;

    FailCategoryColor(String color) {
        this.color = color;
    }
}
