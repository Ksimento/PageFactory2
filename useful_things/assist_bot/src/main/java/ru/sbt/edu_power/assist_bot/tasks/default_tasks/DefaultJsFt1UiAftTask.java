package ru.sbt.edu_power.assist_bot.tasks.default_tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaTeamFt1Job;
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
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IQaJsFt1UiJob;

import java.util.function.BooleanSupplier;

@Slf4j
public class DefaultJsFt1UiAftTask extends AbstractTask<Modal> implements Reset, IJobTask {
    private final Container<QaTeamFt1Job> qaTeamFt1JobContainer;

    public DefaultJsFt1UiAftTask(
            final String stand,
            final Modal modal
    ) {
        super(modal);
        this.qaTeamFt1JobContainer = new Container<>(new QaTeamFt1Job(stand));
    }

    public DefaultJsFt1UiAftTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal
    ) {
        super(queueExecutor, modal);
        this.qaTeamFt1JobContainer = new Container<>(new QaTeamFt1Job(stand));
    }

    public DefaultJsFt1UiAftTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(queueExecutor, modal, isAndCondition, constructConditions);
        this.qaTeamFt1JobContainer = new Container<>(new QaTeamFt1Job(stand));
    }

    public Container<QaTeamFt1Job> getQaTeamFt1JobContainer() {
        return qaTeamFt1JobContainer;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        qaTeamFt1JobContainer.getObject().updateStatus();
        return qaTeamFt1JobContainer.getObject().getStatus();
    }

    @Override
    public void preExecution() {
        new SlackMessage(
                () -> String.format(
                        "Выполнен запуск джобы qa-team-mediateka на стенде %s\n%s",
                        qaTeamFt1JobContainer.getObject().getDEV_STAND(),
                        qaTeamFt1JobContainer.getObject().getBuildUrl()), getModal().getUserId(), Main.QUEUE_EXECUTOR,
                getModal()
        );
    }

    @Override
    public void executeTask() {
        setParams();
        qaTeamFt1JobContainer.getObject().build();
    }

    @Override
    public void postExecution() {
        SlackClient.sendText(
                String.format("Завершено выполнение джобы qa-team-mediateka\n[%s]", qaTeamFt1JobContainer.getObject().getBuildUrl()),
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
            final QaTeamFt1Job qaTeamFt1Job = new QaTeamFt1Job(qaTeamFt1JobContainer.getObject().getDEV_STAND());
            qaTeamFt1JobContainer.setObject(qaTeamFt1Job);
        }
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "JS FT1 UI AFT " + qaTeamFt1JobContainer.getObject().getDEV_STAND();
    }

    private void setParams() {
        qaTeamFt1JobContainer
                .getObject()
                .addParam(QaTeamFt1Job.Params.FRONTEND_DOCKER_TAG, ((IQaJsFt1UiJob) getModal()).getDockerTagFront())
                .addParam(QaTeamFt1Job.Params.JS_UI_TEST_TAG, ((IQaJsFt1UiJob) getModal()).getJsUiFt1TestTag())
                .addParam(QaTeamFt1Job.Params.JS_UI_V4_TEST_TAG, ((IQaJsFt1UiJob) getModal()).getJsUiFt1V4TestTag())
                .addParam(QaTeamFt1Job.Params.JS_UI_FF_TEST_TAG, ((IQaJsFt1UiJob) getModal()).getJsUiFfTestTag())
                .addParam(QaTeamFt1Job.Params.JAVA_API_TEST_TAG, ((IQaJsFt1UiJob) getModal()).getJavaApiTestTag())
                .addParam(QaTeamFt1Job.Params.STAGE_MOCKS, ((IQaJsFt1UiJob) getModal()).getStageMocks())
                .addParam(QaTeamFt1Job.Params.TEST_SET_ID, ((IQaJsFt1UiJob) getModal()).getTestRunKey())
                .addParam(QaTeamFt1Job.Params.CONFIG_BRANCH, ((IQaJsFt1UiJob) getModal()).getConfigBranch());
    }

    @Override
    public String getBuildUrl() {
        return qaTeamFt1JobContainer.getObject().getBuildUrl();
    }
}
