package ru.sbt.edu_power.assist_bot.tasks.test_run.regress;

public enum RegressPreset {
    FULL_REGRESS("Создание полного тест-сета и прогон автотестов"),
    FULL_REGRESS_MFE("Полный регресс только MFE"),
    SMALL_REGRESS("Создание краткого тест-сета (только HIGH приоритет плюс все автоматизированные кейсы) и прогон автотестов"),
    SMALL_REGRESS_MFE("Краткий регресс только MFE"),
    HOT_FIX("Деплой стендов и прогон автотестов, создание тест-сета только под автотесты"),
    HOT_FIX_MFE("Автотесты только MFE");

    private final String description;

    RegressPreset(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
