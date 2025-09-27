package ru.sbt.edu_power.e2e_core.driver_utils;

import ru.sbtqa.tag.qautils.properties.Props;

public class DriverConstants {
    public static final long FREEZE_250_MS = 250L;
    public static final int TIMEOUT = Integer.parseInt(Props.get("timeout"));
    public static final int ELEMENT_WAIT_5SEC = 5;
    public static final int TIME_1SEC = 1;
    public static final long FREEZE_500_MS = 500L;
    public static final long CONVERT_TO_MILLISECONDS = 1000L;
    public static void init() {
        // do nothing
    }
}