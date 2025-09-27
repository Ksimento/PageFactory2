package ru.sbt.edu_power.assist_bot.task_flow;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class QueueExecutor extends ConcurrentLinkedQueue<Task> {
    private static final long serialVersionUID = 7459190670943066410L;
    // будет ли цикл бесконечным
    private final boolean isInfinite;
    private boolean stop;
    private final List<String> taskToRemove = new ArrayList<>();
    private Task executedTask;

    public QueueExecutor(final boolean isInfinite) {
        this.isInfinite = isInfinite;
    }

    public void addTask(final Task task) {
        this.offer(task);
    }

    public Set<Task> getAllTasks() {
        final Set<Task> tasks = new HashSet<>(this);
        if (Objects.nonNull(executedTask)) {
            tasks.add(executedTask);
        }
        tasks.removeIf(t -> taskToRemove.contains(t.getUuid()));
        return tasks;
    }

    public void runQueue() {
        while (!this.isEmpty() || isInfinite) {
            if (stop) {
                log.info("Очередь задач остановлена");
                break;
            }
            executedTask = this.poll();
            if (executedTask == null) {
                ESUtils.freeze(2000);
                continue;
            }
            if (taskToRemove.contains(executedTask.getUuid())) {
                ESUtils.freeze(2000);
                taskToRemove.remove(executedTask.getUuid());
                executedTask = null;
                continue;
            }
            try {
                if (!executedTask.taskCondition()) {
                    ESUtils.freeze(2000);
                    this.offer(executedTask);
                    continue;
                }
                final TaskExecutionStatus status = executedTask.getStatus();
                if (Objects.isNull(status)) {
                    throw new AssistBotException("Задача не передаёт валидный статус выполнения " + executedTask.getTaskName());
                }
                if (status == TaskExecutionStatus.SUCCESS) {
                    log.info("Задача выполнена успешно {}", executedTask.getTaskName());
                    executedTask.postExecution();
                    executedTask.saveLastUpdateTime();
                    continue;
                }
                if (status == TaskExecutionStatus.NOT_STARTED || status == TaskExecutionStatus.REPEATABLE) {
                    executedTask.startTime();
                    if (status == TaskExecutionStatus.NOT_STARTED) {
                        executedTask.preExecution();
                    }
                    log.info("Задача поставлена на выполнение {}", executedTask.getTaskName());
                    executedTask.executeTask();
                } else if (status == TaskExecutionStatus.FAILED) {
                    if (executedTask.hasAttempts()) {
                        log.info("Сбой задачи, перезапуск {}", executedTask.getTaskName());
                        executedTask.reExecuteTask();
                        executedTask.decrementAttempts();
                    } else {
                        executedTask.throwsException();
                    }
                }
                executedTask.saveLastUpdateTime();
                this.offer(executedTask);
                ESUtils.freeze(2000);
            } catch (final Throwable e) {
                log.error(e.getMessage());
                log.error(Stream
                        .of(e.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n")));
                this.offer(executedTask);
                ESUtils.freeze(5000);
            }
            ESUtils.freeze(2000);
        }
    }

    public void addTaskToRemove(final String uuid) {
        taskToRemove.add(uuid);
    }

    @Override
    public String toString() {
        return this.stream()
                   .map(t -> t.getStatus() + ": " + t.getTaskName())
                   .collect(Collectors.joining("\n"));
    }

    public void stop() {
        stop = true;
    }
}
