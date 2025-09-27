package ru.sbt.edu_power.assist_bot.tasks.test_run.regress;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasProjectAndVersions;
import ru.sbt.edu_power.external_services.version_releases.Services;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.NfTestRunCollectionCreate;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.assist_bot.services.jenkins.DockerTagSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.RegressTestRunCreation;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleasesChannelReader;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.DockerTagFromReleasesSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.DockerTagRadioButtonSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.RegressPresetSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SingleCheckboxSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackChannelSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.StandSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.TestRunRiskDependencySelect;
import ru.sbt.edu_power.assist_bot.slack.views.sections.TextSection;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IFullDeploy;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasOverridingReportTs;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasTestRunModel;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IQaJsFt1UiJob;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IQaJsUiJob;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IQaUiParallelJob;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.ISlackChannel;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.ISmokeJob;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunStorage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.CreateTestRunButtonSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.LastReportSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.RegressAssistCheckBoxSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.SmokeTestAcceptPercentSelect;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.StageSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.TestRunSelectSection;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class RegressStartView extends AbstractModal
        implements IFullDeploy, ISmokeJob, IQaUiParallelJob, IQaJsUiJob, IQaJsFt1UiJob, ISlackChannel, IHasTestRunModel,
        IHasOverridingReportTs, IHasProjectAndVersions
{
    private final RegressStartDispatcher dispatcher = new RegressStartDispatcher(this);
    private final Container<TestRunStorage> testRunAnalyticContainer = new Container<>();
    private final Container<RegressTestRunCreation> regressTestRunCreationContainer = new Container<>();
    private final AtomicBoolean generateFinalReport = new AtomicBoolean(false);
    private final ReleasesChannelReader reader = new ReleasesChannelReader(this.getUserId());
    private final Container<NfTestRunCollectionCreate> nfTestRunCollectionCreateContainer = new Container<>();

    private final SlackChannelSelectSection slackChannelSelectSection = new SlackChannelSelectSection();

    private final SingleCheckboxSection singleCheckboxSectionSlackNotify = new SingleCheckboxSection(
            "Нотификация QA JOB (запуск и завершение регресса, статус завершения прогонов)",
            "Включить нотификацию",
            "Если нотификацию выключить, коллеги не узнают о старте и завершении автотестирования",
            true,
            () -> slackChannelSelectSection.getAccessory().isFilled()
    );

    private final RegressPresetSelectSection regressPresetSelectSection = new RegressPresetSelectSection(
            true,
            () -> slackChannelSelectSection.getAccessory().isFilled()
    );

    private final TestRunRiskDependencySelect testRunRiskDependencySelect = new TestRunRiskDependencySelect(
            true,
            () -> regressPresetSelectSection.getAccessory().isFilled()
    );

    private final JiraProjectSelectSection jiraProjectSelectSection =
            new JiraProjectSelectSection(true, () -> testRunRiskDependencySelect.getAccessory().isFilled());

    private final JiraVersionSelectSection jiraVersionSelectSectionDeploy =
            new JiraVersionSelectSection(
                    jiraProjectSelectSection,
                    "Версия для деплоя и запуска тестов"
            );

    private final JiraVersionSelectSection jiraVersionSelectSectionTestRun =
            new JiraVersionSelectSection(
                    jiraProjectSelectSection,
                    "Версия для создания тест-сета"
            );

    private final StageSelectSection stageSelectSection =
            new StageSelectSection(
                    true,
                    () -> jiraVersionSelectSectionDeploy.getAccessory().isFilled()
            );

    private final CreateTestRunButtonSection createTestRunButtonSection = new CreateTestRunButtonSection(
            this,
            true,
            () -> stageSelectSection.getAccessory().isFilled()
    );

    private final TestRunSelectSection testRunSelectSection = new TestRunSelectSection(
            this,
            true,
            () -> stageSelectSection.getAccessory().isFilled(),
            () -> !createTestRunButtonSection.getAccessory().isFilled()
    );

    private final RegressAssistCheckBoxSection regressAssistCheckBoxSection = new RegressAssistCheckBoxSection(
            false,
            () -> createTestRunButtonSection.getAccessory().isFilled(),
            () -> testRunSelectSection.getAccessory().isFilled()
    );

    private final LastReportSelectSection lastReportSelectSection = new LastReportSelectSection(
            slackChannelSelectSection,
            getUserId(),
            true,
            () -> regressAssistCheckBoxSection.getAccessory().isFilled(),
            () -> !regressAssistCheckBoxSection.getAccessory().getValues().contains(RegressAssistants.NONE.name())
    );

    private final SingleCheckboxSection finalReportGenerate = new SingleCheckboxSection(
            "Генерация итогового отчёта",
            "Выполнить итоговый отчёт",
            "По завершении прохождения тест-сета будет сгенерирован отчёт о прохождении регресса и выгружен в Confluence",
            false,
            () -> lastReportSelectSection.getAccessory().isFilled(),
            () -> regressAssistCheckBoxSection.getAccessory().isFilled() &&
             !regressPresetSelectSection.getAccessory().getValue().equals(RegressPreset.HOT_FIX.name()) &&
            regressAssistCheckBoxSection.getAccessory().getValues().contains(RegressAssistants.NONE.name())
    );

    private final SingleCheckboxSection deployStands = new SingleCheckboxSection(
            "Выполнить деплой стендов",
            "Выполнить деплой",
            "Если не указать, шаг деплоя будет пропущен, будут выполнены смок тесты и запущены автотесты",
            true,
            () -> !regressAssistCheckBoxSection.getAccessory().getValues().isEmpty()
    );

    private final DockerTagRadioButtonSection dockerTagRadioButtonSectionFront = new DockerTagRadioButtonSection(
            () -> getDeployVersion().getName(),
            DockerTagSearch.Repo.FRONT,
            "Выбери тег для деплоя на стенд FRONT",
            true,
            () -> deployStands.getAccessory().getBoolean()
    );

    private final DockerTagRadioButtonSection dockerTagRadioButtonSectionBack = new DockerTagRadioButtonSection(
            () -> getDeployVersion().getName(),
            DockerTagSearch.Repo.BACK,
            "Выбери тег для деплоя на стенд BACK",
            true,
            () -> dockerTagRadioButtonSectionFront.getAccessory().isFilled(),
            () -> deployStands.getAccessory().getBoolean(),
            () -> !"none".equals(dockerTagRadioButtonSectionFront.getAccessory().getValue())
    );

    private final DockerTagFromReleasesSelectSection catalogTag = new DockerTagFromReleasesSelectSection(
            Services.CATALOG,
            reader,
            true,
            () -> dockerTagRadioButtonSectionBack.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection userServiceTag = new DockerTagFromReleasesSelectSection(
            Services.EDU_USER_SERVICE,
            reader,
            true,
            () -> catalogTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection orgStructureServiceTag = new DockerTagFromReleasesSelectSection(
            Services.ORG_STRUCTURE,
            reader,
            true,
            () -> userServiceTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection frontMfeTag = new DockerTagFromReleasesSelectSection(
            Services.FRONTEND_MFE,
            reader,
            true,
            () -> orgStructureServiceTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection dataspaceCoreSchoolgroupTag = new DockerTagFromReleasesSelectSection(
            Services.DATASPACE_CORE_SCHOOLGROUP,
            reader,
            true,
            () -> frontMfeTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection apimGraviteeTag = new DockerTagFromReleasesSelectSection(
            Services.APIM_GRAVITEE,
            reader,
            true,
            () -> dataspaceCoreSchoolgroupTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection keycloakSber = new DockerTagFromReleasesSelectSection(
            Services.KEYCLOAK_SBER,
            reader,
            true,
            () -> apimGraviteeTag.getAccessory().isFilled()
    );

    private final SingleCheckboxSection enableUiAftTesting = new SingleCheckboxSection(
            "Ты можешь запустить прогон автотестов если стенды уже задеплоены",
            "Включить автотесты без деплоя",
            "Если прогон не требуется, то оставь поле пустым и модуль можно стартовать",
            true,
            () -> !regressAssistCheckBoxSection.getAccessory().getValues().isEmpty(),
            () -> !deployStands.getAccessory().getBoolean()
    );

    private final SmokeTestAcceptPercentSelect smokeTestAcceptPercentSelect = new SmokeTestAcceptPercentSelect(
            false,
            () -> enableUiAftTesting.getAccessory().getBoolean(),
            () -> deployStands.getAccessory().getBoolean() && dataspaceCoreSchoolgroupTag.getAccessory().isFilled()
    );

    private final TextSection textSectionDefaultDump = new TextSection(
            () -> "Для деплоя будет использован дефолтный дамп. Это временное ограничение",
            true,
            () -> deployStands.getAccessory().getBoolean(),
            () -> smokeTestAcceptPercentSelect.getAccessory().isFilled(),
            () -> dockerTagRadioButtonSectionBack.getAccessory().isFilled()
    );

    private final StandSelectSection standSelectSectionJavaUI = new StandSelectSection(
            "Стенд для запуска Java UI",
            true,
            () -> smokeTestAcceptPercentSelect.getAccessory().isFilled()
    );

    private final StandSelectSection standSelectSectionJsUI = new StandSelectSection(
            "Стенд для запуска JS UI",
            true,
            () -> smokeTestAcceptPercentSelect.getAccessory().isFilled()
    );

    private final StandSelectSection standSelectSectionJsFt1UI = new StandSelectSection(
            "Стенд для запуска JS UI FT1",
            true,
            () -> smokeTestAcceptPercentSelect.getAccessory().isFilled()
    );

    public RegressStartView() {
        reader.updateMessages();
        reader.removeUnsuccessfulServices();
    }

    @Override
    public String getName() {
        return "Создание прогона АФТ";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(dispatcher);
    }

    @Override
    public JiraVersionModel getDeployVersion() {
        return new JiraVersion().getJiraVersionById(
                getJiraProjectId().id,
                jiraVersionSelectSectionDeploy.getAccessory().getValue()
        );
    }

    @Override
    public JiraVersionModel getTestRunVersion() {
        return new JiraVersion().getJiraVersionById(
                getJiraProjectId().id,
                jiraVersionSelectSectionTestRun.getAccessory().getValue()
        );

    }

    @Override
    public String getFrontendBranch() {
        return getDeployVersion().getName();
    }

    @Override
    public String getJsUiTestTag() {
        return regressPresetSelectSection.getAccessory().getValue().contains("MFE") ? "" : "@pipeline @layout";
    }

    @Override
    public String getJsUiV4TestTag() {
        return "@v4_pipeline";
    }

    @Override
    public String getDataSource() {
        return "DEV";
    }

    @Override
    public String getProject() {
        return getJiraProjectId().name();
    }

    @Override
    public String getDockerTagFront() {
        return dockerTagRadioButtonSectionFront.getAccessory().getValue();
    }

    @Override
    public String getJsUiFt1TestTag() {
        return regressPresetSelectSection.getAccessory().getValue().contains("MFE") ? "" : "@FT1";
    }

    @Override
    public String getJsUiFt1V4TestTag() {
        return "@v4_FT1";
    }

    @Override
    public String getJsUiFfTestTag() {
        return regressPresetSelectSection.getAccessory().getValue().contains("MFE") ? "" : "@FF";
    }

    @Override
    public String getJavaApiTestTag() {
        return "";
    }

    @Override
    public String getConfigBranch() {
        return stageSelectSection.getAccessory().getValue();
    }

    @Override
    public boolean getStageMocks() {
        return true;
    }

    @Override
    public String getDockerTagBack() {
        return dockerTagRadioButtonSectionBack.getAccessory().getValue();
    }

    @Override
    public String getDockerTagCatalog() {
        return catalogTag.getAccessory().getValue();
    }

    @Override
    public String getDockerTagUserService() {
        return userServiceTag.getAccessory().getValue();
    }

    @Override
    public String getDockerTagOrgStructure() {
        return orgStructureServiceTag.getAccessory().getValue();
    }

    @Override
    public String getDockerTagMfe() {
        return frontMfeTag.getAccessory().getValue();
    }

    @Override
    public String getSchoolGroupTag() {
        return dataspaceCoreSchoolgroupTag.getAccessory().getValue();
    }

    @Override
    public String getApimGravitee() {
        return apimGraviteeTag.getAccessory().getValue();
    }

    @Override
    public String getKeycloakSber() {
        return keycloakSber.getAccessory().getValue();
    }

    @Override
    public TCFields.ProjectId getJiraProjectId() {
        return TCFields.ProjectId.valueOf(jiraProjectSelectSection.getAccessory().getValue());
    }

    @Override
    public String getTestPack() {
        return regressPresetSelectSection.getAccessory().getValue().contains("MFE") ? "MFE Only" : "Regress";
    }

    @Override
    public boolean getSlackNotify() {
        return singleCheckboxSectionSlackNotify.getAccessory().getBoolean();
    }

    @Override
    public String getTestRunKey() {
        if (createTestRunButtonSection.getAccessory().isFilled()) {
            return regressTestRunCreationContainer.getObject().getBlankTestRunCreation().getTestRunModel().getKey();
        } else {
            return testRunSelectSection.getAccessory().getValue();
        }
    }

    @Override
    public String getSlackChannel() {
        return slackChannelSelectSection.getAccessory().getValue();
    }

    @Override
    public TestRunModel getTestRunModel() {
        return dispatcher.getTestRunModel();
    }


    @Override
    public String getOverridingReportTs() {
        return "skip".equals(lastReportSelectSection.getAccessory().getValue()) ? "" : lastReportSelectSection
                .getAccessory()
                .getValue();
    }
}
