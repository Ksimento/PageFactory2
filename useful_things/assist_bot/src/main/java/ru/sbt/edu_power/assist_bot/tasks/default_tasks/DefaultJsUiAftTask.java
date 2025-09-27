package ru.sbt.edu_power.assist_bot.tasks.default_tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaJsUiJob;
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
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IQaJsUiJob;

import java.util.function.BooleanSupplier;

@Slf4j
public class DefaultJsUiAftTask extends AbstractTask<Modal> implements Reset, IJobTask {
    private final Container<QaJsUiJob> qaJsUiJobContainer;

    public DefaultJsUiAftTask(
            final String stand,
            final Modal modal
    ) {
        super(modal);
        this.qaJsUiJobContainer = new Container<>(new QaJsUiJob(stand));
    }

    public DefaultJsUiAftTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal
    ) {
        super(queueExecutor, modal);
        this.qaJsUiJobContainer = new Container<>(new QaJsUiJob(stand));
    }

    public DefaultJsUiAftTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(queueExecutor, modal, isAndCondition, constructConditions);
        this.qaJsUiJobContainer = new Container<>(new QaJsUiJob(stand));
    }

    public Container<QaJsUiJob> getQaJsUiJobContainer() {
        return qaJsUiJobContainer;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        qaJsUiJobContainer.getObject().updateStatus();
        return qaJsUiJobContainer.getObject().getStatus();
    }

    @Override
    public void preExecution() {
        new SlackMessage(
                () -> String.format(
                        "Выполнен запуск джобы qa-js-ui на стенде %s\n%s",
                        qaJsUiJobContainer.getObject().getDEV_STAND(),
                        qaJsUiJobContainer.getObject().getBuildUrl()), getModal().getUserId(), Main.QUEUE_EXECUTOR,
                getModal()
        );
    }

    @Override
    public void executeTask() {
        setParams();
        qaJsUiJobContainer.getObject().build();
    }

    @Override
    public void postExecution() {
        SlackClient.sendText(
                String.format("Завершено выполнение джобы qa-js-ui\n[%s]", qaJsUiJobContainer.getObject().getBuildUrl()),
                getModal().getUserId()
        );
    }

    @Override
    public void reExecuteTask() {
        reset();
        executeTask();
    }

    @Override
    public void reset() {
        if (getStatus() != TaskExecutionStatus.NOT_STARTED) {
            log.info("Выполняется сброс состояния задачи {}", getTaskName());
            final QaJsUiJob qaJsUiJob = new QaJsUiJob(qaJsUiJobContainer.getObject().getDEV_STAND());
            qaJsUiJobContainer.setObject(qaJsUiJob);
        }
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "JS UI AFT " + qaJsUiJobContainer.getObject().getDEV_STAND();
    }

    private void setParams() {
        qaJsUiJobContainer
                .getObject()
                .addParam(QaJsUiJob.Params.FRONTEND_BRANCH, ((IQaJsUiJob) getModal()).getFrontendBranch())
                .addParam(QaJsUiJob.Params.JS_UI_TEST_TAG, ((IQaJsUiJob) getModal()).getJsUiTestTag())
                .addParam(QaJsUiJob.Params.JS_UI_V4_TEST_TAG, ((IQaJsUiJob) getModal()).getJsUiV4TestTag())
                .addParam(QaJsUiJob.Params.STAGE_DEPLOY, false)
                .addParam(QaJsUiJob.Params.SLACK_NOTIFY, ((IQaJsUiJob) getModal()).getSlackNotify())
                .addParam(QaJsUiJob.Params.TEST_SET_ID, ((IQaJsUiJob) getModal()).getTestRunKey());
    }

    @Override
    public String getBuildUrl() {
        return qaJsUiJobContainer.getObject().getBuildUrl();
    }
}
