package ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces;

public interface IQaJsUiJob {
    String getFrontendBranch();
    String getJsUiTestTag();
    String getJsUiV4TestTag();
    boolean getSlackNotify();
    String getTestRunKey();
}
