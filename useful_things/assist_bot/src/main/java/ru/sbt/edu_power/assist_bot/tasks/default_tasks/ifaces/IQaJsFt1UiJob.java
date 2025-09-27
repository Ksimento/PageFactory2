package ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces;

public interface IQaJsFt1UiJob {
    String getDockerTagFront();
    String getJsUiFt1TestTag();
    String getJsUiFt1V4TestTag();
    String getJsUiFfTestTag();
    String getJavaApiTestTag();
    String getConfigBranch();
    boolean getStageMocks();
    boolean getSlackNotify();
    String getTestRunKey();
}
