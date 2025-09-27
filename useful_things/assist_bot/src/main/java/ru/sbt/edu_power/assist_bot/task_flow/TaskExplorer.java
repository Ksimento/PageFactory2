package ru.sbt.edu_power.assist_bot.task_flow;

import ru.sbt.edu_power.assist_bot.runner.Main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// Диспетчер задач. Хранит все созданные нитки TaskGlue и предоставляет доступ к удалению заданий из Main.QUEUE_EXECUTOR
public final class TaskExplorer {
    private static final TaskExplorer INSTANCE = new TaskExplorer();
    private final Set<TaskGlue> taskGlueList = new HashSet<>();

    private TaskExplorer() {
    }

    public static TaskExplorer getInstance() {
        return INSTANCE;
    }

    public void registerTaskGlue(final TaskGlue taskGlue) {
        taskGlueList.add(taskGlue);
    }

    public Set<TaskGlue> getTaskGlueList() {
        taskGlueList.removeIf(TaskGlue::isComplete);
        taskGlueList.removeIf(TaskGlue::isProtected);
        return taskGlueList;
    }

    public Set<Task> getExecutedTasks() {
        return Main.QUEUE_EXECUTOR.getAllTasks();
    }

    public void stopTask(final String uuid) {
        final Optional<Task> task = new ArrayList<>(Main.QUEUE_EXECUTOR)
                .stream()
                .filter(t -> uuid.equals(t.getUuid()))
                .findFirst();
        task.ifPresent(Main.QUEUE_EXECUTOR::remove);
    }
}
