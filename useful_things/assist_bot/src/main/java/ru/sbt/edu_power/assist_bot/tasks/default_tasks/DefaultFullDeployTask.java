package ru.sbt.edu_power.assist_bot.tasks.default_tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.jobs.FullDeployJob;
import ru.sbt.edu_power.external_services.jenkins.jobs.Job;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.version_releases.Services;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.task_flow.QueueExecutor;
import ru.sbt.edu_power.assist_bot.task_flow.Reset;
import ru.sbt.edu_power.assist_bot.task_flow.templates.SlackMessage;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IFullDeploy;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IJobTask;
import ru.sbt.edu_power.assist_bot.yaml_configurations.DeployVersions;
import ru.sbt.edu_power.assist_bot.yaml_configurations.YamlConfigurator;

import java.util.function.BooleanSupplier;

@Slf4j
public class DefaultFullDeployTask extends AbstractTask<Modal> implements Reset, IJobTask {
    private final Container<FullDeployJob> fullDeployJobContainer;
    private final IFullDeploy params = (IFullDeploy) getModal();
    private final String configName = "edupower_deploy_versions.yaml";
    private final YamlConfigurator yaml = new YamlConfigurator(configName);
    private final DeployVersions config = yaml.load(DeployVersions.class);

    public DefaultFullDeployTask(
            final String stand,
            final Modal modal
    ) {
        super(modal);
        this.fullDeployJobContainer = new Container<>(new FullDeployJob(stand));
    }

    public DefaultFullDeployTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal
    ) {
        super(queueExecutor, modal);
        this.fullDeployJobContainer = new Container<>(new FullDeployJob(stand));
    }

    public DefaultFullDeployTask(
            final String stand,
            final QueueExecutor queueExecutor,
            final Modal modal,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(queueExecutor, modal, isAndCondition, constructConditions);
        this.fullDeployJobContainer = new Container<>(new FullDeployJob(stand));
    }

    @Override
    public TaskExecutionStatus getStatus() {
        fullDeployJobContainer.getObject().updateStatus();
        return fullDeployJobContainer.getObject().getStatus();
    }

    @Override
    public void preExecution() {
        new SlackMessage(
                () -> "Запущен деплой стенда\n" + fullDeployJobContainer.getObject().getBuildUrl(),
                getModal().getUserId(),
                Main.QUEUE_EXECUTOR,
                getModal()
        );
    }

    @Override
    public void executeTask() {
        setParams();
        fullDeployJobContainer.getObject().build();
    }

    @Override
    public void postExecution() {
        SlackClient.sendText(
                "Деплой стенда завершён\n["
                + fullDeployJobContainer.getObject().getBuildUrl() + "]",
                getModal().getUserId()
        );
    }

    @Override
    public void reset() {
        if (getStatus() != TaskExecutionStatus.NOT_STARTED) {
            log.info("Выполняется сброс состояния задачи {}", getTaskName());
            final FullDeployJob fullDeployJob = new FullDeployJob(fullDeployJobContainer.getObject().getDEV_STAND());
            fullDeployJobContainer.setObject(fullDeployJob);
        }
    }

    @Override
    public void reExecuteTask() {
        SlackClient.sendText(
                "Произошёл сбой деплоя [" + fullDeployJobContainer.getObject().getBuildUrl() + "]",
                getModal().getUserId()
        );
        reset();
        executeTask();
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Деплой стенда " + fullDeployJobContainer.getObject().getDEV_STAND();
    }

    private void setParams() {
        // edupower-back
        config.edupower_back.setDeploy(true);
        config.edupower_back.setDockertag(params.getDockerTagBack());

        // edupower-front
        config.edupower_front.setDeploy(true);
        config.edupower_front.setDockertag(params.getDockerTagFront());

        // edupower_catalog
        if (!"skip".equals(params.getDockerTagCatalog())) {
            config.edupower_catalog.setDeploy(true);
            config.edupower_catalog.setDockertag(params.getDockerTagCatalog());
        }

        // MFE
        if (!"skip".equals(params.getDockerTagMfe())) {
            config.getMicrofrontend().getEdupower_frontend_mfe().setDeploy(true);
            config.getMicrofrontend().getEdupower_frontend_mfe().setDockertag(params.getDockerTagMfe());
        }

        // edu-user-service
        setYamlMicroService(Services.EDU_USER_SERVICE, params.getDockerTagUserService());

        // org-structure
        setYamlMicroService(Services.ORG_STRUCTURE, params.getDockerTagOrgStructure());

        // dataspace-core-schoolgroup
        setYamlMicroService(Services.DATASPACE_CORE_SCHOOLGROUP, params.getSchoolGroupTag());

        // APIM_GRAVITEE
        setYamlMicroService(Services.APIM_GRAVITEE, params.getApimGravitee());

        // KEYCLOAK-SBER
        setYamlMicroService(Services.KEYCLOAK_SBER, params.getKeycloakSber());

        fullDeployJobContainer.getObject()
                              .addParam(
                                      new Job.FileField(
                                              FullDeployJob.Params.FILE_DEPLOY_VERSION.name(),
                                              yaml.toString().getBytes(),
                                              configName
                                      )
                              );
        fullDeployJobContainer.getObject()
                              .addParam(
                                      FullDeployJob.Params.ENV_CATALOG,
                                      "skip".equals(params.getDockerTagCatalog())
                              )
                              .addParam(FullDeployJob.Params.CLEANUP_STAND, true)
                              .addParam(FullDeployJob.Params.TIME_SHIFT, "0");
    }

    private void setYamlMicroService(final Services service, final String tag) {
        if (!"skip".equals(tag)) {
            config.getMicroserviceByName(service).setDockertag(tag);
        }
    }

    @Override
    public String getBuildUrl() {
        return fullDeployJobContainer.getObject().getBuildUrl();
    }
}
