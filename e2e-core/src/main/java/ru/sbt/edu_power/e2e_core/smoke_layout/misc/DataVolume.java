package ru.sbt.edu_power.e2e_core.smoke_layout.misc;

public enum DataVolume {
    LOW("@LDV_LOW"),
    NORMAL("@LDV_NORMAL"),
    MAX("@LDV_MAX");

    private final String tag;

    DataVolume(final String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}
