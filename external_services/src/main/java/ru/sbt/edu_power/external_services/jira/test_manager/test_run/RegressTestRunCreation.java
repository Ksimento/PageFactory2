package ru.sbt.edu_power.external_services.jira.test_manager.test_run;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

@Slf4j
public class RegressTestRunCreation implements HasExecutableStatus {
    private static final String REGRESS_FOLDER = "Регресс";
    private static final String RELEASE = "Релиз ";
    private final TCFields.ProjectId PROJECT_ID;
    private final JiraVersionModel JIRA_VERSION;
    private TaskExecutionStatus status;
    private final NewJiraTCCollector collector = new NewJiraTCCollector();
    private BlankTestRunCreation blankTestRunCreation;
    private TestRunModel testRunModel;

    public RegressTestRunCreation(
            final TCFields.ProjectId PROJECT_ID,
            final JiraVersionModel JIRA_VERSION
    ) {
        this.PROJECT_ID = PROJECT_ID;
        this.JIRA_VERSION = JIRA_VERSION;
        status = TaskExecutionStatus.NOT_STARTED;
    }


    public void setBlankTestRunCreation(final BlankTestRunCreation blankTestRunCreation) {
        this.blankTestRunCreation = blankTestRunCreation;
    }

    public BlankTestRunCreation getBlankTestRunCreation() {
        return blankTestRunCreation;
    }


    public void create(final Preset preset, final TCFields.Risk minRisk) {
        status = TaskExecutionStatus.IN_PROGRESS;
        createEmptyTestRun(generateTestRunName(preset, minRisk));
        switch (preset) {
            case FULL_REGRESS:
                updateRiskFields(minRisk);
                createFullRegress(minRisk, false);
                break;
            case FULL_REGRESS_MFE:
                updateRiskFields(minRisk);
                createFullRegress(minRisk, true);
                break;
            case SMALL_REGRESS:
                updateRiskFields(minRisk);
                createSmallRegress(minRisk, false);
                break;
            case SMALL_REGRESS_MFE:
                updateRiskFields(minRisk);
                createSmallRegress(minRisk, true);
                break;
            case HIGH_PRIORITY_ONLY:
                updateRiskFields(minRisk);
                createHighPriorityRegress(minRisk, false);
                break;
            case HIGH_PRIORITY_ONLY_MFE:
                updateRiskFields(minRisk);
                createHighPriorityRegress(minRisk, true);
                break;
            case AUTO_ONLY:
                collectAllAutomatedTests(false);
                fillTestRun();
                break;
            case AUTO_ONLY_MFE:
                collectAllAutomatedTests(true);
                fillTestRun();
                break;
        }
    }

    private String generateTestRunName(final Preset preset, final TCFields.Risk minRisk) {
        final String risk = minRisk != TCFields.Risk.LOWEST ? " (риск от " + minRisk.getValue() + ")" : "";
        switch (preset) {
            case FULL_REGRESS:
                return String.format("Полный регресс %s%s", JIRA_VERSION.getName(), risk);
            case FULL_REGRESS_MFE:
                return String.format("Полный регресс MFE %s%s", JIRA_VERSION.getName(), risk);
            case SMALL_REGRESS:
                return String.format("Краткий регресс %s%s", JIRA_VERSION.getName(), risk);
            case SMALL_REGRESS_MFE:
                return String.format("Краткий регресс MFE %s%s", JIRA_VERSION.getName(), risk);
            case HIGH_PRIORITY_ONLY:
                return String.format("Только высокий приоритет %s%s", JIRA_VERSION.getName(), risk);
            case HIGH_PRIORITY_ONLY_MFE:
                return String.format("Только высокий приоритет MFE %s%s", JIRA_VERSION.getName(), risk);
            case AUTO_ONLY:
                return String.format("Только автотесты %s", JIRA_VERSION.getName());
            case AUTO_ONLY_MFE:
                return String.format("Только автотесты MFE %s", JIRA_VERSION.getName());
            default:
                throw new ExternalServicesException("Неизвестный пресет " + preset);
        }
    }

    // создаём тест-сет для полного регресса
    public void createFullRegress(final TCFields.Risk minRisk, final boolean mfeOnly) {
        // все автоматизированные
        collectAllAutomatedTests(mfeOnly);
        final TCQueryBuilder queryBuilder = new TCQueryBuilder();
        // стандартные поля
        addFields(queryBuilder, mfeOnly);
        // не автоматизированные
        addNotAutomated(queryBuilder);
        // с учётом риска
        addRiskLevel(queryBuilder, minRisk);
        collector.collect(queryBuilder);
        fillTestRun();
    }

    // создаём тест-сет с кейсами самого высокого приоритета
    public void createHighPriorityRegress(final TCFields.Risk minRisk, final boolean mfeOnly) {
        final TCQueryBuilder queryBuilder = new TCQueryBuilder();
        addFields(queryBuilder, mfeOnly);
        addRiskLevel(queryBuilder, minRisk);
        queryBuilder.addField(TCFields.PRIORITY, TCFields.Priority.HIGH);
        collector.collect(queryBuilder);
        fillTestRun();
    }

    // создаём тест-сет для краткого регресса - все автоматизированные кейсы плюс все ручные кейсы высокого приоритета
    public void createSmallRegress(final TCFields.Risk minRisk, final boolean mfeOnly) {
        collectAllAutomatedTests(mfeOnly);
        final TCQueryBuilder queryBuilder = new TCQueryBuilder();
        addFields(queryBuilder, mfeOnly);
        addNotAutomated(queryBuilder);
        addRiskLevel(queryBuilder, minRisk);
        queryBuilder.addField(TCFields.PRIORITY, TCFields.Priority.HIGH);
        collector.collect(queryBuilder);
        fillTestRun();
    }

    // если в поле выбора рисков выбрано что-то кроме "Все тест-кейсы" -
    // сначала обновляем риски в тест-кейсах
    private void updateRiskFields(final TCFields.Risk minRisk) {
//        if (minRisk != TCFields.Risk.LOWEST
//        ) {
//            new TestCaseRiskAssessment().collect(PROJECT_ID.name());
//        }
    }

    // создаю пустой тест-сет
    private void createEmptyTestRun(final String testSetName) {
        blankTestRunCreation = new BlankTestRunCreation(
                PROJECT_ID,
                JIRA_VERSION,
                REGRESS_FOLDER,
                getTestSetDirectory()
        );
        testRunModel = blankTestRunCreation.create(testSetName);
        log.info("Создан пустой тест-сет {} {}", testRunModel.getName(), testRunModel.getKey());
    }

    // заполняем тест-сет тест-кейсами
    private void fillTestRun() {
        log.info("Всего получено {} тест-кейсов для добавления в тест-сет", collector.asMap().size());

        // набиваю пустой тест-сет тест-кейсами
        final BulkTestRunUpdate bulkTestRunUpdate = new BulkTestRunUpdate(testRunModel);
        bulkTestRunUpdate.addItems(collector.asMap().values()).execute();
        status = TaskExecutionStatus.SUCCESS;
    }

    // набиваем весь набор обязательных полей для фильтра тест-кейсов в регресс
    private void addFields(final TCQueryBuilder queryBuilder, final boolean mfeOnly) {
        queryBuilder.addField(TCFields.ARCHIVED, false)
                    .addField(TCFields.STATUS, TCFields.Status.APPROVED, TCFields.Status.NEED_REFACTORING)
                    .addField(TCFields.TEST_VIEW, TCFields.TestView.U_I)
                    .addField(TCFields.TEST_TYPE, TCFields.TestType.REGRESS)
                    .addField(TCFields.PROJECT_ID, PROJECT_ID)
                    .addField(TCFields.FOLDER, NewJiraTCCollector.getInstance().getFolderId("Регресс", PROJECT_ID.id));
        if (mfeOnly) {
            addMfeFilter(queryBuilder);
        }
    }

    private void addNotAutomated(final TCQueryBuilder queryBuilder) {
        queryBuilder.addField(
                TCFields.AUTOMATED_STATUS,
                TCFields.AutomatedStatus.EXCLUDED,
                TCFields.AutomatedStatus.NOT,
                TCFields.AutomatedStatus.NOT_REQUIRED
        );
    }

    private void addRiskLevel(final TCQueryBuilder queryBuilder, final TCFields.Risk minRisk) {
        switch (minRisk) {
            case LOWEST:
                queryBuilder.addField(TCFields.RISK, TCFields.Risk.UNDEFINED);
                queryBuilder.addField(TCFields.RISK, TCFields.Risk.LOWEST);
            case LOW:
                queryBuilder.addField(TCFields.RISK, TCFields.Risk.LOW);
            case MEDIUM:
                queryBuilder.addField(TCFields.RISK, TCFields.Risk.MEDIUM);
            case HIGH:
                queryBuilder.addField(TCFields.RISK, TCFields.Risk.HIGH);
            case CRITICAL:
                queryBuilder.addField(TCFields.RISK, TCFields.Risk.CRITICAL);
            default:
        }
    }

    private void collectAllAutomatedTests(final boolean mfeOnly) {
        final TCQueryBuilder queryBuilder = new TCQueryBuilder();
        addFields(queryBuilder, mfeOnly);
        queryBuilder.addField(
                TCFields.AUTOMATED_STATUS,
                TCFields.AutomatedStatus.ON_AUTOMATE,
                TCFields.AutomatedStatus.YES
        );
        collector.collect(queryBuilder);
    }

    private void addMfeFilter(final TCQueryBuilder queryBuilder) {
        queryBuilder.addField(TCFields.LABELS, "V4", "v4", "ЛАЙТ", "ПМО");
    }

    // генерируем название поддиректории для создания там регресса
    private String getTestSetDirectory() {
        return RELEASE + JIRA_VERSION.getName().replace("r/", "").split("-")[0];
    }

    @Override
    public void updateStatus() {
        // do nothing;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }

    public NewJiraTCCollector getCollector() {
        return collector;
    }

    public enum Preset {
        FULL_REGRESS("Полный набор тест-кейсов"),
        FULL_REGRESS_MFE("Полный набор тест-кейсов с лэйблами MFE"),
        SMALL_REGRESS("Все автоматизированные кейсы плюс кейсы HIGH приоритета"),
        SMALL_REGRESS_MFE("Все автоматизированные кейсы плюс кейсы HIGH приоритета с лэйблами MFE"),
        HIGH_PRIORITY_ONLY("Только тесты HIGH приоритета"),
        HIGH_PRIORITY_ONLY_MFE("Только тесты HIGH приоритета с лэйблами MFE"),
        AUTO_ONLY("Только автоматизированные кейсы"),
        AUTO_ONLY_MFE("Только автоматизированные кейсы с лэйблами MFE");

        private final String description;

        Preset(final String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
