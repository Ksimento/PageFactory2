package ru.sbt.edu_power.assist_bot.tasks.default_tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaJavaUiJob;
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
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IQaUiJob;

import java.util.function.BooleanSupplier;

@Slf4j
public class DefaultJavaUiAftTask extends AbstractTask<Modal> implements Reset, IJobTask {
    private final Container<QaJavaUiJob> qaJavaUiJobContainer;
    private final String javaUiTestTag;

    public DefaultJavaUiAftTask(
            final String stand,
            final String javaUiTestTag,
            final QueueExecutor queueExecutor,
            final Modal modal
    ) {
        super(queueExecutor, modal);
        this.qaJavaUiJobContainer = new Container<>(new QaJavaUiJob(stand));
        this.javaUiTestTag = javaUiTestTag;
    }

    public DefaultJavaUiAftTask(
            final String stand,
            final String javaUiTestTag,
            final QueueExecutor queueExecutor,
            final Modal modal,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(queueExecutor, modal, isAndCondition, constructConditions);
        this.qaJavaUiJobContainer = new Container<>(new QaJavaUiJob(stand));
        this.javaUiTestTag = javaUiTestTag;
    }

    public Container<QaJavaUiJob> getQaJavaUiJobContainer() {
        return qaJavaUiJobContainer;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        qaJavaUiJobContainer.getObject().updateStatus();
        return qaJavaUiJobContainer.getObject().getStatus();
    }

    @Override
    public void preExecution() {
        new SlackMessage(
                () -> String.format(
                        "Выполнен запуск джобы qa-java-ui на стенде %s с тегом %s\n%s",
                        qaJavaUiJobContainer.getObject().getDEV_STAND(),
                        javaUiTestTag,
                        qaJavaUiJobContainer.getObject().getBuildUrl()), getModal().getUserId(), Main.QUEUE_EXECUTOR,
                getModal()
        );
    }

    @Override
    public void executeTask() {
        setParams();
        qaJavaUiJobContainer.getObject().build();
    }

    @Override
    public void postExecution() {
        SlackClient.sendText(
                String.format("Завершено выполнение джобы qa-java-ui\n[%s]", qaJavaUiJobContainer.getObject().getBuildUrl()),
                getModal().getUserId()
        );
    }

    @Override
    public void reset() {
        if (getStatus() != TaskExecutionStatus.NOT_STARTED) {
            log.info("Выполняется сброс состояния задачи {}", getTaskName());
            final QaJavaUiJob qaJavaUiJob = new QaJavaUiJob(qaJavaUiJobContainer
                    .getObject()
                    .getDEV_STAND());
            qaJavaUiJobContainer.setObject(qaJavaUiJob);
        }
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
        return "Запуск Java UI";
    }

    private void setParams() {
        qaJavaUiJobContainer.getObject()
                            .addParam(
                                    QaJavaUiJob.Params.FRONTEND_BRANCH,
                                    ((IQaUiJob) getModal()).getFrontendBranch()
                            )
                            .addParam(
                                    QaJavaUiJob.Params.JIRA_PROJECT_KEY,
                                    ((IQaUiJob) getModal()).getJiraProjectId().name()
                            )
                            .addParam(
                                    QaJavaUiJob.Params.JAVA_UI_TEST_TAG,
                                    javaUiTestTag
                            )
                            .addParam(
                                    QaJavaUiJob.Params.SLACK_NOTIFY,
                                    ((IQaUiJob) getModal()).getSlackNotify()
                            )
                            .addParam(
                                    QaJavaUiJob.Params.TEST_SET_ID,
                                    ((IQaUiJob) getModal()).getTestRunKey()
                            )
                            .addParam(
                                    QaJavaUiJob.Params.HACK_SQL,
                                    ((IQaUiJob) getModal()).getHackSql()
                            )
        ;
    }

    @Override
    public String getBuildUrl() {
        return qaJavaUiJobContainer.getObject().getBuildUrl();
    }
}
