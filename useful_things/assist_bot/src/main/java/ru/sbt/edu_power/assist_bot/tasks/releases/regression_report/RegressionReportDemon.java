package ru.sbt.edu_power.assist_bot.tasks.releases.regression_report;

import ru.sbt.edu_power.assist_bot.roles.roles.UserRolesRepository;
import ru.sbt.edu_power.assist_bot.task_flow.IDemon;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;

public class RegressionReportDemon implements IDemon {
    @Override
    public void init() {
        final RegressionReportTask task = new RegressionReportTask();
        new TaskGlue("Обобщённый отчёт в #release_candidates-notify", UserRolesRepository.getAdminUser(), true)
                .add(task)
                .addQueueCompleteCondition(() -> false)
                .execute();
    }
}
