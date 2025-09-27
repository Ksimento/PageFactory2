package ru.sbt.edu_power.assist_bot.tasks.default_tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaJavaUiParallelJob;
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
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IQaUiParallelJob;

import java.util.function.BooleanSupplier;

@Slf4j
public class DefaultJavaUiParallelAftTask extends AbstractTask<Modal> implements Reset, IJobTask {
    private final Container<QaJavaUiParallelJob> qaJavaUiParallelJobContainer;

    public DefaultJavaUiParallelAftTask(
            final String stand,
            final Modal modal
    ) {
        super(modal);
        this.qaJavaUiParallelJobContainer = new Container<>(new QaJavaUiParallelJob(stand));
    }

    public DefaultJavaUiParallelAftTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal
    ) {
        super(queueExecutor, modal);
        this.qaJavaUiParallelJobContainer = new Container<>(new QaJavaUiParallelJob(stand));
    }

    public DefaultJavaUiParallelAftTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(queueExecutor, modal, isAndCondition, constructConditions);
        this.qaJavaUiParallelJobContainer = new Container<>(new QaJavaUiParallelJob(stand));
    }

    public Container<QaJavaUiParallelJob> getQaJavaUiParallelJobContainer() {
        return qaJavaUiParallelJobContainer;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        qaJavaUiParallelJobContainer.getObject().updateStatus();
        return qaJavaUiParallelJobContainer.getObject().getStatus();
    }

    @Override
    public void preExecution() {
        new SlackMessage(
                () -> String.format(
                        "Выполнен запуск джобы qa-java-ui-parallel на стенде %s\n%s",
                        qaJavaUiParallelJobContainer.getObject().getDEV_STAND(),
                        qaJavaUiParallelJobContainer.getObject().getBuildUrl()), getModal().getUserId(), Main.QUEUE_EXECUTOR,
                getModal()
        );
    }

    @Override
    public void executeTask() {
        setParams();
        qaJavaUiParallelJobContainer.getObject().build();
    }

    @Override
    public void postExecution() {
        SlackClient.sendText(
                String.format("Завершено выполнение джобы qa-java-ui-parallel\n[%s]", qaJavaUiParallelJobContainer.getObject().getBuildUrl()),
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
            final QaJavaUiParallelJob qaJavaUiParallelJob = new QaJavaUiParallelJob(qaJavaUiParallelJobContainer
                    .getObject()
                    .getDEV_STAND());
            qaJavaUiParallelJobContainer.setObject(qaJavaUiParallelJob);
        }
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Java UI Parallel " + qaJavaUiParallelJobContainer.getObject().getDEV_STAND();
    }

    private void setParams() {
        qaJavaUiParallelJobContainer.getObject()
                                    .addParam(
                                            QaJavaUiParallelJob.Params.FRONTEND_BRANCH,
                                            ((IQaUiParallelJob) getModal()).getFrontendBranch()
                                    )
                                    .addParam(
                                            QaJavaUiParallelJob.Params.JIRA_PROJECT_KEY,
                                            ((IQaUiParallelJob) getModal()).getJiraProjectId().name()
                                    )
                                    .addParam(
                                            QaJavaUiParallelJob.Params.TEST_PACK,
                                            ((IQaUiParallelJob) getModal()).getTestPack()
                                    )
                                    .addParam(
                                            QaJavaUiParallelJob.Params.SLACK_NOTIFY,
                                            ((IQaUiParallelJob) getModal()).getSlackNotify()
                                    )
                                    .addParam(
                                            QaJavaUiParallelJob.Params.TEST_SET_ID,
                                            ((IQaUiParallelJob) getModal()).getTestRunKey()
                                    );
    }

    @Override
    public String getBuildUrl() {
        return qaJavaUiParallelJobContainer.getObject().getBuildUrl();
    }
}
