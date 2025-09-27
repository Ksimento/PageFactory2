package ru.sbt.edu_power.assist_bot.tasks.releases.regression_report;

import ru.sbt.edu_power.assist_bot.task_flow.Task;
import ru.sbt.edu_power.assist_bot.task_flow.TaskExplorer;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.RegressAssistantTask;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RegressionReportCollector extends ArrayList<RegressionData> {
    private static final long serialVersionUID = -7643157174650208767L;

    public void updateCollection() {
        removeIf(rd -> !rd.getTaskGlue().isActive());
        TaskExplorer.getInstance().getTaskGlueList().forEach(tg -> {
            if (stream().map(RegressionData::getTaskGlue).anyMatch(tg::equals)) {
                return;
            }
            final List<Task> taskList = tg.getQueue().stream()
                    .map(TaskGlue.Dependent::getDependent)
                    .collect(Collectors.toList());
            if (taskList.stream().anyMatch(t -> t instanceof RegressAssistantTask)) {
                add(new RegressionData(tg));
            }
        });
    }
}
