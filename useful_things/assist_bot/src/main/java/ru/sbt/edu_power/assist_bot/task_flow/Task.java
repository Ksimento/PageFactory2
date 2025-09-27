package ru.sbt.edu_power.assist_bot.task_flow;

import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;

import java.time.Duration;
import java.util.function.Supplier;

public interface  Task {
    // статус выполнения задания
    TaskExecutionStatus getStatus();

    // статус == SUCCESS
    boolean isTaskComplete();

    // действия перед стартом задачи
    void preExecution();

    // исполняемый код задания
    void executeTask();

    // действия после завершения задачи
    void postExecution();

    // условие при котором задание должно начать выполняться
    boolean taskCondition();

    // код, который выполняется если предыдущий запуск завершился со статусом FAILED
    void reExecuteTask();

    // функция уменьшения количества попыток
    void decrementAttempts();

    // функция возвращает true если ещё есть попытки
    boolean hasAttempts();

    // будет вызвана в случае исчерпания доступных попыток
    void throwsException();

    // функция сохраняет время последнего запуска задания
    void saveLastUpdateTime();

    // возвращает функцию для рассчёта следующего периода простоя
    Supplier<Long> getIdleDuration();

    // возвращает true если время простоя вышло (то-есть требуется запустить выполнение задания)
    boolean isIdleTimeOut();

    // возвращает количество запусков задания
    int taskExecutionCounter();

    // инкремент количества запусков задания
    void taskExecutionIncrement();

    // возвращает название задания
    String getTaskName();

    QueueExecutor getQueueExecutor();

    Modal getModal();

    String getUuid();

    // возвращает длительность выполнения задачи
    Duration getTaskExecutionDuration();

    // устанавливает время старта исполнения задачи
    void startTime();

    void setStatus(final TaskExecutionStatus status);
}
