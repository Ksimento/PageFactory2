package ru.sbt.edu_power.assist_bot.tasks.default_tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaUiSmokeJob;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.task_flow.QueueExecutor;
import ru.sbt.edu_power.assist_bot.task_flow.Reset;
import ru.sbt.edu_power.assist_bot.task_flow.templates.SlackMessage;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IJobTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.ISmokeJob;

import java.util.function.BooleanSupplier;

@Slf4j
public class DefaultSmartSmokeTask extends AbstractTask<Modal> implements Reset, IJobTask {
    private final Container<QaUiSmokeJob> qaUiSmokeJobContainer;
    private TaskExecutionStatus status;

    public DefaultSmartSmokeTask(
            final String stand,
            final Modal modal
    ) {
        super(modal);
        qaUiSmokeJobContainer = new Container<>(new QaUiSmokeJob(stand));
    }

    public DefaultSmartSmokeTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal
    ) {
        super(queueExecutor, modal);
        qaUiSmokeJobContainer = new Container<>(new QaUiSmokeJob(stand));
    }

    public DefaultSmartSmokeTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(queueExecutor, modal, isAndCondition, constructConditions);
        qaUiSmokeJobContainer = new Container<>(new QaUiSmokeJob(stand));
    }

    public Container<QaUiSmokeJob> getQaUiSmokeJobContainer() {
        return qaUiSmokeJobContainer;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        if (status == TaskExecutionStatus.SUCCESS) {
            return status;
        }
        qaUiSmokeJobContainer.getObject().updateStatus();
        status =  qaUiSmokeJobContainer.getObject().getStatus();
        return status;
    }

    @Override
    public void setStatus(final TaskExecutionStatus status) {
        this.status = status;
    }

    @Override
    public void preExecution() {
        new SlackMessage(
                () -> String.format(
                        "Выполнен запуск джобы qa-java-ui-smoke на стенде %s\n%s",
                        qaUiSmokeJobContainer.getObject().getDEV_STAND(),
                        qaUiSmokeJobContainer.getObject().getBuildUrl()), getModal().getUserId(), Main.QUEUE_EXECUTOR,
                getModal()
        );
    }

    @Override
    public void executeTask() {
        setParams();
        qaUiSmokeJobContainer.getObject().build();
    }

    @Override
    public void postExecution() {
        SlackClient.sendText(
                String.format("Завершено выполнение джобы qa-java-ui-smoke\n[%s]", qaUiSmokeJobContainer.getObject().getBuildUrl()),
                getModal().getUserId()
        );
    }

    @Override
    public void reExecuteTask() {
        reset();
        executeTask();
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Запуск смок тестов " + qaUiSmokeJobContainer.getObject().getDEV_STAND();
    }

    @Override
    public void reset() {
        if (getStatus() != TaskExecutionStatus.NOT_STARTED) {
            log.info("Выполняется сброс состояния задачи {}", getTaskName());
            final QaUiSmokeJob qaUiSmokeJob = new QaUiSmokeJob(qaUiSmokeJobContainer.getObject().getDEV_STAND());
            qaUiSmokeJobContainer.setObject(qaUiSmokeJob);
        }
    }

    private void setParams() {
        qaUiSmokeJobContainer.getObject()
                             .addParam(
                                     QaUiSmokeJob.Params.FRONTEND_BRANCH,
                                     ((ISmokeJob) getModal()).getFrontendBranch()
                             )
                             .addParam(QaUiSmokeJob.Params.DATA_SOURCE, ((ISmokeJob) getModal()).getDataSource())
                             .addParam(QaUiSmokeJob.Params.PROJECT, ((ISmokeJob) getModal()).getProject());
    }

    @Override
    public String getBuildUrl() {
        return qaUiSmokeJobContainer.getObject().getBuildUrl();
    }
}
