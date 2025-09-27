package ru.sbt.edu_power.assist_bot.tasks.system.memory_usage;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;

import java.util.function.Supplier;

@Slf4j
public class MemoryUsageTask extends AbstractTask<AbstractModal> {
    private final MemoryUsageStorage memoryUsageStorage = new MemoryUsageStorage();
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;

    protected MemoryUsageTask() {
        super(null);
    }

    public MemoryUsageStorage getMemoryUsageStorage() {
        return memoryUsageStorage;
    }

    @Override
    public Supplier<Long> getIdleDuration() {
        return () -> getMillisFromMinutes(10);
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }

    @Override
    public void preExecution() {

    }

    @Override
    public void executeTask() {
        if (status == TaskExecutionStatus.NOT_STARTED) {
            status = TaskExecutionStatus.REPEATABLE;
        }
        memoryUsageStorage.slice();
    }

    @Override
    public void postExecution() {

    }

    @Override
    public void reExecuteTask() {

    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Memory control";
    }
}
