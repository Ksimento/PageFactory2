package ru.sbt.edu_power.assist_bot.tasks.test_run.regress;

public enum RegressAssistants {
    NONE("Не использовать ассистента"),
    IFT_NEED_TEST("Контроль статусов IFT, Need Test, In QA"),
    REGRESSION("Контроль прохождения регресса");

    private final String description;

    RegressAssistants(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
