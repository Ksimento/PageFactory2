package ru.sbt.edu_power.assist_bot.tasks.releases.regression_report;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.task_flow.Task;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.AnalyticUtils;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.RegressAssistantTask;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class RegressionData {
    private final TaskGlue taskGlue;
    private final RegressAssistantTask regressAssistantTask;

    public RegressionData(final TaskGlue taskGlue) {
        this.taskGlue = taskGlue;
        final Optional<Task> taskOptional = taskGlue.getQueue().stream()
                                                    .map(TaskGlue.Dependent::getDependent)
                                                    .filter(task -> task instanceof RegressAssistantTask)
                                                    .findFirst();
        regressAssistantTask = (RegressAssistantTask) taskOptional
                .orElseThrow(() -> new AssistBotException("Не удалось получить экземпляр RegressAssistantTask"));
    }

    public int getPassed() {
        return regressAssistantTask.getTestRunStorage().isEmpty() ? 0 :
                regressAssistantTask.getTestRunStorage().getLastSlice().getStatusToExecutionMap()
                                    .entrySet()
                                    .stream()
                                    .filter(e -> AnalyticUtils.isInCompleteStatus(e.getKey()))
                                    .map(Map.Entry::getValue)
                                    .mapToInt(List::size)
                                    .sum();
    }

    public int getAll() {
        return regressAssistantTask.getTestRunStorage().isEmpty() ? 0 :
                regressAssistantTask.getTestRunStorage().getLastSlice().getStatusToExecutionMap()
                                    .values()
                                    .stream()
                                    .mapToInt(List::size)
                                    .sum();
    }
}
