package ru.sbt.edu_power.external_services.jenkins.allure;

import java.util.Arrays;
import java.util.List;

public enum TestType {
    JAVA_UI,
    JS_UI,
    JAVA_API;

    public static TestType determineByTags(final List<String> tags) {
        final String[] javaTests = {"regress", "regress_S21"};
        Arrays.sort(javaTests);
        final String[] jsTests = {"@v4_pipeline", "@pipeline", "@FT1"};
        Arrays.sort(jsTests);
        if (tags.stream().anyMatch(tag -> Arrays.binarySearch(javaTests, tag) >= 0)) {
            return JAVA_UI;
        }
        if (tags.stream().anyMatch(tag -> Arrays.binarySearch(jsTests, tag) >= 0)) {
            return JS_UI;
        }
        return JAVA_API;
    }
}
