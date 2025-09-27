package ru.sbt.edu_power.assist_bot.task_flow;

import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class  AbstractTask<T extends Modal> implements Task {
    private Boolean isAndCondition;
    private BooleanSupplier[] constructConditions;
    private int attempts = 3;
    private QueueExecutor queueExecutor;
    private final T modal;
    private int executionCounter;
    private LocalDateTime lastUpdateTime;
    private final String uuid = UUID.randomUUID().toString();
    private LocalDateTime executionStartTime;

    @Override
    public Duration getTaskExecutionDuration() {
        final LocalDateTime lastTime;
        final TaskExecutionStatus status = getStatus();
        switch (status) {
            case SUCCESS:
            case FAILED:
                lastTime = lastUpdateTime;
                break;
            default:
                lastTime = LocalDateTime.now();
                break;
        }
        return Objects.isNull(executionStartTime) ? Duration.ZERO : Duration.between(executionStartTime, lastTime);
    }

    @Override
    public void startTime() {
        executionStartTime = LocalDateTime.now();
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void saveLastUpdateTime() {
        lastUpdateTime = LocalDateTime.now();
    }

    @Override
    public Supplier<Long> getIdleDuration() {
        throw new AssistBotException("Метод 'getIdleDuration' не поддерживается, требуется его переопределить");
    }

    @Override
    public boolean isIdleTimeOut() {
        return lastUpdateTime.plus(getIdleDuration().get(), ChronoUnit.MILLIS).isBefore(LocalDateTime.now());
    }

    @Override
    public int taskExecutionCounter() {
        return executionCounter;
    }

    @Override
    public void taskExecutionIncrement() {
        executionCounter++;
    }

    protected AbstractTask(final T modal) {
        this.modal = modal;
    }

    protected AbstractTask(final QueueExecutor queueExecutor, final T modal) {
        this.queueExecutor = queueExecutor;
        if (Objects.nonNull(queueExecutor)) {
            queueExecutor.addTask(this);
        }
        this.modal = modal;
    }

    protected AbstractTask(final QueueExecutor queueExecutor, final T modal, final boolean isAndCondition, final BooleanSupplier... constructConditions) {
        this.queueExecutor = queueExecutor;
        this.isAndCondition = isAndCondition;
        this.constructConditions = constructConditions;
        Objects.requireNonNull(queueExecutor);
        queueExecutor.addTask(this);
        this.modal = modal;
    }

    @Override
    public boolean isTaskComplete() {
        return getStatus() == TaskExecutionStatus.SUCCESS;
    }

    @Override
    public boolean taskCondition() {
        if (isAndCondition == null) {
            return true;
        }
        if (isAndCondition) {
            return Stream.of(constructConditions).allMatch(BooleanSupplier::getAsBoolean);
        }
        return Stream.of(constructConditions).anyMatch(BooleanSupplier::getAsBoolean);
    }

    @Override
    public void decrementAttempts() {
        attempts--;
    }

    @Override
    public boolean hasAttempts() {
        return attempts > 0;
    }

    @Override
    public QueueExecutor getQueueExecutor() {
        return queueExecutor;
    }

    @Override
    public final T getModal() {
        return modal;
    }

    protected long getMillisFromMinutes(final int minutes) {
        return minutes * 60 * 1000L;
    }

    @Override
    public void setStatus(final TaskExecutionStatus status) {
        throw new AssistBotException("Устновка статуса не поддерживается задачей '" + getTaskName() + "'");
    }
}
