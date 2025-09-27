package ru.sbt.edu_power.e2e_core.layout.enums;

public enum UserAgent {
    MOBILE("Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1"),
    DEFAULT("");

    private final String userAgentName;

    UserAgent(final String userAgentName) {
        this.userAgentName = userAgentName;
    }

    public String getUserAgentName() {
        return userAgentName;
    }
}
