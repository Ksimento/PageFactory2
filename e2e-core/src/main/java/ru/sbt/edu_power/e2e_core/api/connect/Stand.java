package ru.sbt.edu_power.e2e_core.api.connect;

public class Stand {
    private static final String STAND_URL_PROPERTY_NAME = "webdriver.starting.url";
    public static final String STAND_URL = System.getProperty(STAND_URL_PROPERTY_NAME);
}
