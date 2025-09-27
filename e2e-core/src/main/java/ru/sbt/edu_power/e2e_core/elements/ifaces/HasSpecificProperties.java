package ru.sbt.edu_power.e2e_core.elements.ifaces;

import java.util.Map;

public interface HasSpecificProperties {
    void validateSpecificProperties(Map<String, String> propertiesMap);
    Map<String, String> readSpecificProperties();
}
