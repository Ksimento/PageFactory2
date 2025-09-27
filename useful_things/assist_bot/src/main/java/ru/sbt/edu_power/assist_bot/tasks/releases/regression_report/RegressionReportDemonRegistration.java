package ru.sbt.edu_power.assist_bot.tasks.releases.regression_report;

import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackDemonRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.IDemon;

public class RegressionReportDemonRegistration extends AbstractSlackDemonRegistration {
    @Override
    public String getName() {
        return "Общий отчёт о регрессе в #release_candidates-notify";
    }

    @Override
    public boolean isDemon() {
        return true;
    }

    @Override
    public IDemon getDemon() {
        return new RegressionReportDemon();
    }
}
