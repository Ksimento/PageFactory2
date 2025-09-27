package ru.sbt.edu_power.e2e_core.reports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import io.cucumber.datatable.DataTable;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.SystemProperties;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.GenerateRtmAuditTableNF;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.GenerateRtmAuditTableRegress;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.GenerateRtmTable;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.GenerateV3V4RTMTable;
import ru.sbt.edu_power.confluence_reporting.regress_metrics.MeasureHandleRegressTime;
import ru.sbt.edu_power.confluence_reporting.regress_metrics.MeasureHandleRegressTimeByQaEngineers;
import ru.sbt.edu_power.e2e_core.auxiliary.StandConfigurationDiff;
import ru.sbt.edu_power.e2e_core.confluence_integration.GeneratePlatformGraph;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbt.edu_power.e2e_core.error_processing.NotCriticalErrorAccumulator;
import ru.sbt.edu_power.e2e_core.features_utils.Feature;
import ru.sbt.edu_power.e2e_core.features_utils.FeaturesParser;
import ru.sbt.edu_power.e2e_core.features_utils.ScenariosParser;
import ru.sbt.edu_power.e2e_core.layout.OldDataSetRemover;
import ru.sbt.edu_power.e2e_core.page_tags.FeaturesTagWriter;
import ru.sbt.edu_power.e2e_core.page_tags.PageChildren;
import ru.sbt.edu_power.e2e_core.page_tags.PageGraphCollector;
import ru.sbt.edu_power.e2e_core.smoke.SmokeFeaturesGenerator;
import ru.sbt.edu_power.e2e_core.smoke_layout.SmokeLayoutFeaturesGenerator;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.jira.test_manager.TMFields;
import ru.sbt.edu_power.external_services.jira.test_manager.TMTestCaseHandler;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModelFolder;
import ru.sbtqa.tag.pagefactory.HTMLPage;
import ru.sbtqa.tag.pagefactory.annotations.ActionTitle;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public abstract class CoreJiraReports extends HTMLPage {
    public TCFields.ProjectId getJiraProject() {
        throw new AutotestError("Необходимо переопределить этот метод и возвращать из него валидный проект");
    }

    @ActionTitle("проставляет теги в фичах использующих тестирование верстки")
    public void layoutTestingTags(final String globalTag, final String layoutTag) {
        final FeaturesParser parser = new FeaturesParser();
        parser.filterFeaturesByTag(FeaturesParser.Operator.OR, globalTag);
        final List<File> features = parser.asFileList();
        features.forEach(f -> {
            final ScenariosParser scenariosParser = new ScenariosParser(f.toPath());
            scenariosParser.parse();
            scenariosParser.getFeature()
                           .getScenarios()
                           .stream()
                           .map(JsonElement::getAsJsonObject)
                           .filter(scenario -> {
                               for (final JsonElement e : scenario
                                       .getAsJsonArray(Feature.ScenarioSection.STEPS.name())) {
                                   if (e.getAsString().contains("И проверяет параметры верстки")) {
                                       return true;
                                   }
                               }
                               return false;
                           })
                           .forEach(scenario -> {
                               if (!scenariosParser
                                       .getFeature()
                                       .getScenarioData(scenario, Feature.ScenarioSection.TAGS)
                                       .contains(layoutTag)
                               ) {
                                   scenariosParser
                                           .getFeature()
                                           .addScenarioSection(
                                                   scenario,
                                                   Feature.ScenarioSection.TAGS,
                                                   layoutTag
                                           );
                                   scenariosParser.getFeature().saveToDisk();
                               }
                           });
        });
    }

    @ActionTitle("находит Smoke тесты без параметров")
    public void fillSmokeNullIdInTeam() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        IntStream.range(0,27).forEach(i->stringBuilder.append(" :heavy_minus_sign: "));
        String text = "@channel\n" + new SmokeFeaturesGenerator(false).checkSmokeInNullParams() + "\n\n" + stringBuilder +
                               new SmokeFeaturesGenerator(true).checkSmokeInNullParams();
        final File report = new File("target/stepInNullParams.txt");
        FileUtils.write(
                report,
                text,
                StandardCharsets.UTF_8
        );
    }

    @ActionTitle("генерирует smoke тесты команды для DEV")
    public void generateSmokeDevInTeam(final String team) {
        new SmokeFeaturesGenerator(false, team).generateFeaturesInTeam();
    }

    @ActionTitle("генерирует smoke тесты команды для PROD")
    public void generateSmokeProdInTeam(final String team) {
        new SmokeFeaturesGenerator(true, team).generateFeaturesInTeam();
    }

    @ActionTitle("находит дубли тест-кейсов в фичах")
    public void doubledTestCases(final String globalTag, final String projectKey) {
        final FeaturesParser parser = new FeaturesParser();
        final List<String> tags = parser
                .filterFeaturesByTag(FeaturesParser.Operator.OR, globalTag)
                .filterTagsByMask(FeaturesParser.Operator.OR, "@" + projectKey + "*")
                .asDuplicationTagList();
        if (tags.isEmpty()) {
            log.info("Ура! В фичах {} дублирующиеся теги не найдены", globalTag);
            Allure.addAttachment(String.format("Ура! В фичах %s дублирующиеся теги не найдены", globalTag), "");
        } else {
            log.info("В фичах {} найдено {} дублирующихся тегов:\n{}", globalTag, tags.size(), String.join("\n", tags));
            Allure.addAttachment(
                    String.format("В фичах %s найдено %d дублирующихся тегов:\n", globalTag, tags.size()),
                    String.join("\n", tags)
            );
            NotCriticalErrorAccumulator.setNotCriticalError("Найдены задублированные теги");
            NotCriticalErrorAccumulator.setStepBroken();
        }

    }

    @ActionTitle("находит фичи с параметрами")
    public void featuresWithParams(final String globalTag) {
        final FeaturesParser parser = new FeaturesParser();
        final List<String> nameList = parser
                .filterFeaturesByTag(FeaturesParser.Operator.OR, globalTag)
                .filterFeaturesByContent(FeaturesParser.Operator.OR, "Примеры:")
                .asFileNameList();
        if (nameList.isEmpty()) {
            log.info("Ура! Фичи {} с параметрами отсутствуют", globalTag);
            Allure.addAttachment(String.format("Ура! Фичи %s с параметрами отсутствуют", globalTag), "");
        } else {
            log.info(
                    "{} фичей {} с параметрами (создают дублирование тегов при прогоне):\n{}",
                    nameList.size(),
                    globalTag,
                    String.join("\n", nameList)
            );
            Allure.addAttachment(
                    String.format(
                            "%d фичей %s с параметрами (создают дублирование тегов при прогоне)",
                            nameList.size(),
                            globalTag
                    ),
                    String.join("\n", nameList)
            );
            NotCriticalErrorAccumulator.setNotCriticalError("Найдены фичи с параметрами");
            NotCriticalErrorAccumulator.setStepBroken();
        }
    }

    @ActionTitle("вычисляет расхождение в тегах с актуальной РТМ")
    public void matchTagsWithRTM(final String globalTag, final String team) {
        final NewJiraTCCollector collector = new NewJiraTCCollector();
        final TCQueryBuilder queryBuilder = new TCQueryBuilder()
                .addField(TCFields.ARCHIVED, false)
                .addField(TCFields.PROJECT_ID, getJiraProject())
                .addField(
                        TCFields.TEST_TYPE,
                        TCFields.TestType.REGRESS,
                        TCFields.TestType.N_F,
                        TCFields.TestType.NOT_FOR_REGRESS
                )
                .addField(TCFields.TEST_VIEW, TCFields.TestView.U_I, TCFields.TestView.LAYOUT)
                .addField(
                        TCFields.AUTOMATED_STATUS,
                        TCFields.AutomatedStatus.ON_AUTOMATE,
                        TCFields.AutomatedStatus.YES,
                        TCFields.AutomatedStatus.EXCLUDED
                )
                .addField(
                        TCFields.FRAMEWORK,
                        TCFields.Framework.SELENIUM,
                        TCFields.Framework.EMPTY
                );
        if (!team.isEmpty()) {
            queryBuilder.addField(TCFields.TEAM, team);
        }

        collector.collect(queryBuilder);
        final Map<String, TestCaseModel> dataFromJira = collector.asMap();

        final FeaturesParser parser = new FeaturesParser();
        final List<String> featuresTagList = parser
                .filterFeaturesByTag(FeaturesParser.Operator.OR, globalTag)
                .filterTagsByMask(FeaturesParser.Operator.OR, "@" + getJiraProject().name() + "*")
                .asUniqueTagList();
        final List<String> tempTagList = new ArrayList<>(featuresTagList);
        tempTagList.forEach(tag -> {
            if (dataFromJira.containsKey(tag)) {
                dataFromJira.remove(tag);
                featuresTagList.remove(tag);
            }
        });

        if (dataFromJira.isEmpty()) {
            log.info("Ура! Все автоматизированные тест-кейсы РТМ отмечены в фичах {}", globalTag);
            Allure.addAttachment("Ура! Все автоматизированные тест-кейсы РТМ отмечены в фичах " + globalTag, "");
        } else {
            log.info(
                    "РТМ {} содержит {} автоматизированных тест-кейсов, которые отсутствуют в автотестах:\n",
                    globalTag,
                    dataFromJira.size()
            );
            Allure.addAttachment(
                    String.format(
                            "РТМ %s содержит %d автоматизированных тест-кейсов, которые отсутствуют в автотестах:\n",
                            globalTag,
                            dataFromJira.size()
                    ),
                    String.join("\n", dataFromJira.keySet())
            );
            NotCriticalErrorAccumulator.setNotCriticalError("РТМ содержит тесты-кейсы, которые отсутствуют в автотестах");
        }
        if (featuresTagList.isEmpty()) {
            log.info("Ура! Все теги в фичах {} содержатся в РТМ среди автоматизированных тест-кейсов", globalTag);
            Allure.addAttachment(String.format(
                    "Ура! Все теги в фичах %s содержатся в РТМ среди автоматизированных тест-кейсов",
                    globalTag
            ), "");
        } else {
            final List<String> errorList = new ArrayList<>();
            for (final String tag : featuresTagList) {
                final TestCaseModel model;
                try {
                    model = collector.getById(tag);
                    errorList.add(getIncorrectInfo(model));
                } catch (final JiraConnectionException e) {
                    errorList.add(tag + ": не существует в Jira");
                }
            }

            log.info(
                    "В тестах {} выявлены проблемы",
                    globalTag
            );
            Allure.addAttachment(
                    String.format(
                            "%d автоматизированных тест-кейсов %s не отмеченые в РТМ:\n",
                            featuresTagList.size(),
                            globalTag
                    ),
                    String.join("\n\n", errorList)
            );
            NotCriticalErrorAccumulator.setNotCriticalError("Автотесты не отмечены в РТМ");
        }
    }

    private String getIncorrectInfo(final TestCaseModel model) {
        String message = "";
        final TCFields.TestView testView = model.getTestView();
        if (testView != TCFields.TestView.U_I && testView != TCFields.TestView.LAYOUT) {
            final String tv = Objects.isNull(testView) ? "не задана" : testView.value;
            message += "\n\tВид теста: " + tv;
        }

        final TCFields.TestType testType = model.getTestType();
        if (
                testType != TCFields.TestType.REGRESS &&
                testType != TCFields.TestType.N_F &&
                testType != TCFields.TestType.NOT_FOR_REGRESS
        ) {
            final String tt = Objects.isNull(testType) ? "не задан" : testType.value;
            message += "\n\tТип теста: " + tt;
        }

        if (Objects.isNull(model.getTeam()) || model.getTeam().isEmpty()) {
            message += "\n\tКоманда: не заполнено";
        }

        final List<TCFields.Framework> framework = model.getFramework();
        if (framework == null || framework.size() != 1 || framework.get(0) != TCFields.Framework.SELENIUM) {
            final String f;
            if (Objects.isNull(framework) || framework.isEmpty()) {
                f = "не задан";
            } else if (framework.get(0) == TCFields.Framework.EMPTY || framework.size() > 1) {
                f = "неверное значение поля (либо двойное значение, либо EMPTY)";
            } else {
                f = framework
                        .stream()
                        .map(TCFields.Framework::name)
                        .collect(Collectors.joining(", "));
            }
            message += "\n\tФреймворк: " + f;

        }

        if (model.getArchived()) {
            message += "\n\tТест-кейс в архиве";
        }

        final TCFields.AutomatedStatus automatedStatus = model.getAutomatedStatus();
        if (
                automatedStatus != TCFields.AutomatedStatus.EXCLUDED &&
                automatedStatus != TCFields.AutomatedStatus.ON_AUTOMATE &&
                automatedStatus != TCFields.AutomatedStatus.YES
        ) {
            final String as = Objects.isNull(automatedStatus) ? "не задан" : automatedStatus.value;
            message += "\n\tСтатус автоматизации: " + as;
        }
        if (!message.isEmpty()) {
            return model.getKey() + ":" + message;
        } else {
            return model.getKey() + " не удалось локализовать причину расхождения между РТМ и фичами";
        }
    }

    @ActionTitle("находит фичи без тест-кейсов по глобальному тегу")
    public void featuresWithoutTestCases(final String globalTag, final String projectKey) {
        final FeaturesParser parser = new FeaturesParser()
                .filterFeaturesByTag(FeaturesParser.Operator.OR, globalTag)
                .filterFeaturesByTag(FeaturesParser.Operator.NOT, "@" + projectKey + "*");
        final List<String> features = parser.asFileNameList();
        if (features.isEmpty()) {
            log.info("Ура! Во всех фичах \"{}\" есть теги тест-кейсов", globalTag);
            Allure.attachment(String.format("Ура! Во всех фичах \"%s\" есть теги тест-кейсов", globalTag), "");
        } else {
            log.info(
                    "{} фичей с тегом \"{}\" не содержат тест-кейсы:\n{}",
                    features.size(),
                    globalTag,
                    String.join("\n", features)
            );
            Allure.attachment(
                    String.format("%d фичей с тегом \"%s\" не содержат тест-кейсы", features.size(), globalTag),
                    String.join("\n", features)
            );
            NotCriticalErrorAccumulator.setNotCriticalError("Фичи без тегов тест-кейсов");
        }
        NotCriticalErrorAccumulator.setStepBroken();
    }

    /**
     * Сценарий автоматически обновляет в фичах с тегами тест-кейсов теги функциональных команд
     *
     * @param globalTag тег, по которому искать все обрабатываемые фичи
     * @param dataTable мапа из названия функциональной команды и соответствующего ей тега
     */
    @ActionTitle("обновляет теги функциональных команд и смешанных сценариев")
    public void teamTagsUpdate(final String globalTag, final DataTable dataTable) {
        final Map<String, String> teamToTagNameMap = new HashMap<>(dataTable.asMap(String.class, String.class));
        final FeaturesParser parser = new FeaturesParser();
        parser.filterFeaturesByTag(
                FeaturesParser.Operator.AND,
                "@" + getJiraProject() + "*",
                globalTag
        );
        final List<File> featuresList = parser.asFileList();
        featuresList.forEach(feature ->
                parser.removeTags(true, feature, "@F_")
        );
        // название тега, который присваивается фиче со смешанными тест-кейсами из разных команд
        final String MIXED_TEAM_TAG_NAME = "@T_Mixed";
        // список команд реально использующихся, для проверки наличия лишних команд в сценарии
        final Set<String> usedTeam = new HashSet<>();
        featuresList.forEach(feature -> {
            final List<String> featureTags = parser.getTagsFromFile(feature);
            final List<String> currentTeamTagList = featureTags
                    .stream()
                    .filter(tag -> tag.startsWith("@T_"))
                    .collect(Collectors.toList());
            final String currentTeamTag = currentTeamTagList.size() == 1 ? currentTeamTagList.get(0) : "";
            featureTags.removeIf(tag -> !tag.startsWith("@" + getJiraProject()));
            featureTags.replaceAll(tag -> tag.replace("@", ""));
            final String featureTeam = featureTags
                    .stream()
                    .map(NewJiraTCCollector.getInstance()::getById)
                    .map(TestCaseModel::getTeam)
                    .reduce((a, b) -> a.equals(b) ? b : "")
                    .orElse("");
            if (featureTeam.isEmpty()) {
                if (!currentTeamTag.equals(MIXED_TEAM_TAG_NAME)) {
                    parser.removeTags(true, feature, "@T_");
                    parser.writeTag(MIXED_TEAM_TAG_NAME, globalTag, feature);
                }
            } else {
                if (!teamToTagNameMap.containsKey(featureTeam)) {
                    throw new AutotestError("В сценарии отсутствует команда " + featureTeam);
                }
                usedTeam.add(featureTeam);
                if (!currentTeamTag.equals(teamToTagNameMap.get(featureTeam))) {
                    parser.removeTags(true, feature, "@T_");
                    parser.writeTag(teamToTagNameMap.get(featureTeam), globalTag, feature);
                }
            }
        });
        final ErrorCollector errorCollector = new ErrorCollector();
        teamToTagNameMap.keySet().forEach(team -> errorCollector.assertTrue(
                String.format("Команда \"%s\" не представлена в тест-кейсах", team),
                usedTeam.contains(team)
        ));
        errorCollector.assertAll();
    }


    /**
     * Сценарий отбирает в джире автоматизированные тест-кейсы и проставляет автоматически поле Framework
     * Если поле уже заполнено значением Selenium или Selenium+JS - тест-кейс будет пропущен
     * Если поле пустое - будет заполнено Selenium, если стоит JS - будет выставлено Selenium+JS
     *
     * @param globalTag  тег, по которому искать фичи
     * @param projectKey код проектной области джиры, в которой выполняется работа. Например "EDU"
     */
    @ActionTitle("обновляет поле Framework")
    public void updateFrameworkField(final String globalTag, final String projectKey) {
        final TCQueryBuilder queryBuilder = new TCQueryBuilder()
                .addField(TCFields.ARCHIVED, false)
                .addField(TCFields.FRAMEWORK, TCFields.Framework.SELENIUM)
                .addField(TCFields.PROJECT_ID, getJiraProject());
        NewJiraTCCollector.getInstance().collect(queryBuilder);
        final Map<String, TestCaseModel> currentSeleniumTestCaseMap = TCFilter
                .of(NewJiraTCCollector.getInstance().asMap())
                .filter(queryBuilder)
                .toMap();
        final FeaturesParser parser = new FeaturesParser();
        parser.filterFeaturesByTag(FeaturesParser.Operator.OR, globalTag);
        final List<String> tagList = parser
                .collectTags()
                .asUniqueTagList()
                .stream()
                .filter(t -> t.startsWith(getJiraProject().name()))
                .collect(Collectors.toList());
        final List<String> messages = new ArrayList<>();
        final List<String> testArchived = new ArrayList<>();
        // Удяляем фреймворк из тест-кейсов, если у нас нет автоматизированного кейса
        currentSeleniumTestCaseMap.forEach((tc, model) -> {
            if (!tagList.contains(tc)) {
                final List<TCFields.Framework> currentFramework = model.getFramework();
                if (Objects.isNull(currentFramework)) {
                    return;
                }
                if (model.getArchived()) {
                    testArchived.add(tc);
                    return;
                }
                if (currentFramework.contains(TCFields.Framework.JS) ||
                    currentFramework.contains(TCFields.Framework.SELENIUM_JS)) {
                    TMTestCaseHandler.updateFrameworkField(tc, TCFields.Framework.JS.value);
                    messages.add(String.format("Для тест-кейса %s установлен фреймворк JS", tc));
                } else {
                    // если тест-кейс в статусе На автоматизации - не сбрасываем значение фреймворка
                    if (model.getAutomatedStatus() == TCFields.AutomatedStatus.ON_AUTOMATE) {
                        return;
                    }
                    TMTestCaseHandler.updateFrameworkField(tc, TCFields.Framework.EMPTY.value);
                    messages.add(String.format("Для тест-кейса %s установлен фреймворк EMPTY", tc));
                }
            }
        });
        // Удаляем из списка все теги, в которых указан фреймворк SELENIUM
        tagList.removeIf(currentSeleniumTestCaseMap::containsKey);
        // Записываем новое значение фреймворка
        final List<String> testInJsError = new ArrayList<>();
        tagList.forEach(tag -> {
            final List<TCFields.Framework> currentFramework = NewJiraTCCollector
                    .getInstance()
                    .getById(tag)
                    .getFramework();
            if (NewJiraTCCollector.getInstance().getById(tag).getArchived()) {
                testArchived.add(tag);
                return;
            }
            if (Objects.nonNull(currentFramework) && currentFramework.contains(TCFields.Framework.JS)) {
                testInJsError.add(tag);
                return;
            }
            TMTestCaseHandler.updateFrameworkField(tag, TCFields.Framework.SELENIUM.value);
            messages.add(String.format("Для тест-кейса %s установлен фреймворк SELENIUM", tag));
        });
        log.info("Обновлено {} тест-кейсов", messages.size());
        log.info("Найдено {} ошибок в фреймворке", testInJsError.size());
        if (!messages.isEmpty()) {
            Allure.addAttachment("Обновленные значения поля Framework", String.join("\n", messages));
        }
        if (!testInJsError.isEmpty()) {
            Allure.addAttachment(
                    "Тесты отмеченные фреймворком JS не могут быть одновременно автоматизированы на SELENIUM",
                    String.join("\n", testInJsError)
            );
            NotCriticalErrorAccumulator.setNotCriticalError(
                    "Тесты отмеченные фреймворком JS не могут быть одновременно автоматизированы на SELENIUM");
        }
        if (!testArchived.isEmpty()) {
            Allure.addAttachment(
                    "Тесты в архиве и не могут быть обновлены",
                    String.join("\n", testArchived)
            );
            NotCriticalErrorAccumulator.setNotCriticalError("Тесты в архиве и не могут быть обновлены");
        }

        NotCriticalErrorAccumulator.setStepBroken();
    }

    /**
     * Сценарий записывает в фичи тег приоритета, который установлен в тест-кейсе.
     * Если в фиче несколько тест-кейсов с разным приоритетом - запишется наивысший
     *
     * @param globalTag глобальный тег. Будут обработаны только фичи с этим тегом
     * @param dataTable Таблица должна содержать название проектной области в джире в первой строке
     *                  В остальных: вес (чем больше число тем выше вес), название приоритета из джиры, название
     *                  тега для этого приоритета для фичи
     */
    @ActionTitle("обновляет теги приоритета")
    public void prioritiesTagUpdate(final String globalTag, final DataTable dataTable) {
        final List<List<String>> data = new ArrayList<>(dataTable.asLists());
        // Забираю название проекта из data и удаляю эту строку
        final String projectKey = data.get(0).get(2);
        data.remove(0);
        // Список всех файлов с globalTag
        final FeaturesParser parser = new FeaturesParser();
        final List<File> features = parser
                .filterFeaturesByTag(FeaturesParser.Operator.OR, globalTag)
                .asFileList();
        // Priority из джиры -> {"tag" -> dataTableTag; "weight" -> dataTableWeight}
        final Map<String, Map<String, String>> jiraPriorityNameToTagWeightMap = new HashMap<>();
        // @tag -> Jira priority name
        final Map<String, List<String>> featurePriorityTagToJiraPriorityName = new HashMap<>();
        // заполняю обе мапы
        data.forEach(row -> {
            final Map<String, String> map = new HashMap<>();
            map.put("tag", row.get(2));
            map.put("weight", row.get(0));
            jiraPriorityNameToTagWeightMap.put(
                    TMFields.Priority.getByPriorityName(row.get(1)).getPriorityName(),
                    map
            );
            if (!featurePriorityTagToJiraPriorityName.containsKey(row.get(2))) {
                featurePriorityTagToJiraPriorityName.put(row.get(2), new ArrayList<>());
            }
            featurePriorityTagToJiraPriorityName.get(row.get(2)).add(row.get(1));
        });
        // Перебираю все фичи, определяю наивысший приоритет по тегам и записываю тег приоритета в фичу
        features.forEach(file -> {
            final List<String> tags = parser.getTagsFromFile(file);
            final String currentPriorityTag = tags
                    .stream()
                    .filter(tag -> tag.startsWith("@P_"))
                    .findFirst()
                    .orElse("");
            // Initial priority value = "Low"
            final AtomicReference<String> jiraPriorityName = new AtomicReference<>(data.get(2).get(1));
            // Перебираю все теги в фиче и выбираю наивысший приоритет из присутствующих
            tags.stream()
                .filter(tag -> tag.startsWith("@" + projectKey))
                .map(tag -> tag.replace("@", ""))
                .forEach(tag -> {
                    final TestCaseModel model = NewJiraTCCollector.getInstance().getById(tag);
                    final int currentWeight = Integer.parseInt(
                            jiraPriorityNameToTagWeightMap
                                    .get(jiraPriorityName.get())
                                    .get("weight")
                    );
                    final int weightByTag;
                    if (model.getArchived()) {
                        weightByTag = 1;
                    } else {
                        weightByTag = Integer.parseInt(
                                jiraPriorityNameToTagWeightMap
                                        .get(model.getPriority().getName())
                                        .get("weight")
                        );
                    }
                    if (weightByTag > currentWeight) {
                        jiraPriorityName.set(model.getPriority().getName());
                    }
                });
            // Записываю тег приоритета если он отсутствует или отличается от текущего
            if ("".equals(currentPriorityTag)
                || featurePriorityTagToJiraPriorityName.get(currentPriorityTag) == null
                || !featurePriorityTagToJiraPriorityName.get(currentPriorityTag).contains(jiraPriorityName.get())) {
                parser.removeTags(true, file, "@P_");
                parser.writeTag(jiraPriorityNameToTagWeightMap.get(jiraPriorityName.get()).get("tag"), globalTag, file);
            }
        });
    }

    @ActionTitle("обновляет данные по РТМ")
    public void updateRTMDataTable(final String pageID, final String projectKey, final String minAutomatePercent) {
        new GenerateRtmTable(pageID, projectKey, false, Integer.parseInt(minAutomatePercent)).generate();
    }

    @ActionTitle("обновляет данные по РТМ + НФ")
    public void updateRTMNFDataTable(final String pageID, final String projectKey) {
        new GenerateRtmTable(pageID, projectKey, true, 0).generate();
    }

    @ActionTitle("обновляет данные по РТМ + НФ V3 V4")
    public void updateRTMNFV3V4DataTable(final String pageID, final String projectKey) {
        new GenerateV3V4RTMTable(pageID, projectKey).generate();
    }

    @ActionTitle("обновляет данные по аудиту РТМ")
    public void updateRtmAuditDataTable(final String pageID, final String project) {
        new GenerateRtmAuditTableRegress(pageID, project).generate();
    }

    @ActionTitle("обновляет данные по аудиту РТМ НФ")
    public void updateRtmAuditDataTableNF(final String pageID, final String project) {
        new GenerateRtmAuditTableNF(pageID, project).generate();
    }

    @ActionTitle("проставляет теги страниц и обновляет структуру платформы")
    public void addFunctionalGroups(final String pageId, final DataTable dataTable) {
        final PageGraphCollector collector = new PageGraphCollector();
        collector.collect();
        collector.buildChildrenList();
        final List<PageChildren> pageChildrenList = collector.getPageChildren();
        final FeaturesTagWriter featuresTagWriter = new FeaturesTagWriter(
                dataTable.asList().toArray(new String[]{}),
                pageChildrenList
        );
        featuresTagWriter.writeTags();
        new GeneratePlatformGraph(pageId, pageChildrenList, featuresTagWriter.getTagCounter()).generate();
    }

    @ActionTitle("определяет разницу в настройках стендов")
    public void diffConfigurations(final DataTable data) {
        final StandConfigurationDiff diff = new StandConfigurationDiff(new ArrayList<>(data.asList()));
        diff.restore();
        diff.excludeConfigWithFileLinks();
        diff.compare();
        diff.generateSQLScript();
    }

    @ActionTitle("обновляет скип статус тестов по тегу в проекте")
    public void setSkipTags(final String skipTagName, final String globalTag) {
        final TCQueryBuilder queryBuilder = new TCQueryBuilder()
                .addField(TCFields.ARCHIVED, false)
                .addField(TCFields.PROJECT_ID, getJiraProject())
                .addField(TCFields.AUTOMATED_STATUS, TCFields.AutomatedStatus.EXCLUDED);
        final Map<String, TestCaseModel> skippedTests = TCFilter
                .of(
                        NewJiraTCCollector
                                .getInstance()
                                .collect(queryBuilder)
                                .asMap()
                )
                .filter(queryBuilder)
                .toMap();
        final FeaturesParser featuresParser = new FeaturesParser();
        featuresParser.filterFeaturesByTag(FeaturesParser.Operator.OR, globalTag);
        final List<File> fileList = featuresParser.asFileList();
        final List<Feature> featureList = new ArrayList<>();
        fileList.forEach(file -> {
            final ScenariosParser scenariosParser = new ScenariosParser(file.toPath());
            scenariosParser.parse();
            featureList.add(scenariosParser.getFeature());
        });

        final AtomicInteger tagsAdded = new AtomicInteger(0);
        final AtomicInteger tagsRemoved = new AtomicInteger(0);
        for (final Feature feature : featureList) {
            final List<String> globalTagList = feature.getGlobalTags();
            final boolean hasGlobalSkipTag = feature.ifHasGlobalTag(skipTagName).isPresent();
            final boolean hasExclusionGlobalTags =
                    globalTagList.stream()
                                 .map(tag -> tag.replace("@", ""))
                                 .anyMatch(skippedTests::containsKey);
            if (hasExclusionGlobalTags && !hasGlobalSkipTag) {
                feature.addFeatureSection(Feature.Section.GLOBAL_TAGS, skipTagName);
                tagsAdded.incrementAndGet();
                feature.saveToDisk();
            } else if (!hasExclusionGlobalTags && hasGlobalSkipTag) {
                feature.removeFeatureData(Feature.Section.GLOBAL_TAGS, skipTagName);
                tagsRemoved.incrementAndGet();
                feature.saveToDisk();
            }
            final List<JsonElement> scenarios = feature.getScenarios();
            scenarios.forEach(scenario -> {
                final boolean hasExclusionTag =
                        feature.getScenarioData(scenario, Feature.ScenarioSection.TAGS)
                               .stream()
                               .map(tag -> tag.replace("@", ""))
                               .anyMatch(skippedTests::containsKey);
                final boolean hasSkipTag = feature.ifScenarioHasTag(scenario, skipTagName).isPresent();
                if (hasExclusionTag && !hasSkipTag) {
                    feature.addScenarioSection(scenario.getAsJsonObject(), Feature.ScenarioSection.TAGS, skipTagName);
                    tagsAdded.incrementAndGet();
                    feature.saveToDisk();
                } else if (!hasExclusionTag && hasSkipTag) {
                    feature.removeScenarioData(scenario.getAsJsonObject(), Feature.ScenarioSection.TAGS, skipTagName);
                    tagsRemoved.incrementAndGet();
                    feature.saveToDisk();
                }
            });
        }
        log.info("Добавлено тегов @skip {}", tagsAdded.get());
        log.info("Удалено тегов @skip {}", tagsRemoved.get());
    }

    @SuppressWarnings("unchecked")
    @ActionTitle("удаляет не активные профили из файла")
    public void deleteNotActiveUsers() {
        final List<File> featuresList = new FeaturesParser().getFeaturesList();
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final String filePath = SystemProperties.getProperty("users.resources");
            final File file = new File(filePath);
            final ObjectNode node = (ObjectNode) mapper.readTree(file);
            Map<String, Object> mapUser = new HashMap<>();
            for (final JsonNode jsonNode : node) {
                mapUser = mapper.convertValue(jsonNode, Map.class);
            }
            final Map<String, Object> mapUsersDelete = new HashMap<>(mapUser);
            log.info("Следующие юзеры будут удалены:");
            for (final String user : mapUser.keySet()) {
                for (final File fileFeature : featuresList) {
                    if (FileUtils.readFileToString(fileFeature, StandardCharsets.UTF_8).contains("\"" + user + "\"")) {
                        mapUsersDelete.remove(user);
                        break;
                    }
                }
            }
            for (final String user : mapUsersDelete.keySet()) {
                log.info(user);
                ((ObjectNode) node.get("predefined_data")).remove(user);
            }
            try (final BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @ActionTitle("удаляет не активные даты-сеты")
    public void deleteNotActiveDataSets() {
        new OldDataSetRemover().execute();
    }

    @ActionTitle("генерирует smoke автотесты DEV")
    public void smartSmokeTestGenerationDev() {
        new SmokeFeaturesGenerator(false).generateFeatures();
    }

    @ActionTitle("генерирует smoke автотесты PROD")
    public void smartSmokeTestGenerationProd() {
        new SmokeFeaturesGenerator(true).generateFeatures();
    }

    @ActionTitle("генерирует статистику по регрессу по командам")
    public void generateRegressStatByTeams(final String page) {
        new MeasureHandleRegressTime(page).generate();
    }

    @ActionTitle("генерирует статистику по регрессу по сотрудникам")
    public void generateRegressStatByMembers(final String page) {
        new MeasureHandleRegressTimeByQaEngineers(page).generate();
    }

    @ActionTitle("генерирует РТМ отчёт с исключением кейсов V3 для ученика и родителя")
    public void generateRtmReportWithoutV3(
            final String pageId,
            final String projectKey,
            final String minAutomatePercent
    ) {
        final GenerateRtmTable generateRtmTable = new GenerateRtmTable(
                pageId,
                projectKey,
                false,
                Integer.parseInt(minAutomatePercent)
        );
        final Consumer<Map<String, Map<String, TestCaseModel>>> alterDataFunction = (map) -> map.forEach((team, tcMap) -> {
            final List<String> v3Keys = tcMap
                    .values()
                    .stream()
                    .filter(t -> t.getLabels().contains("V3") ||
                                 t.getLabels().contains("v3"))
                    .filter(t -> t.getFolder()
                                  .getParentsFolders()
                                  .stream()
                                  .map(TestCaseModelFolder::getName)
                                  .anyMatch(name -> "Ученик".equals(name) ||
                                                    "Родитель".equals(name))
                    )
                    .map(TestCaseModel::getKey)
                    .collect(Collectors.toList());
            final List<String> apiTests = tcMap
                    .values()
                    .stream()
                    .filter(t -> t.getTestView() == TCFields.TestView.API)
                    .map(TestCaseModel::getKey)
                    .collect(Collectors.toList());
            v3Keys.forEach(tcMap::remove);
            apiTests.forEach(tcMap::remove);
        });
        generateRtmTable.getRtmCollector().alterTestCaseMap(alterDataFunction);
        generateRtmTable.generate();
    }

    @ActionTitle("генерирует smoke тесты верстки для PROD")
    public void generateSmokeLayoutTestsProd(final String project) {
        new SmokeLayoutFeaturesGenerator(true, project).generate();
    }

    @ActionTitle("генерирует smoke тесты верстки для PROD")
    public void generateSmokeLayoutTestsProd(final String project, final String className) {
        new SmokeLayoutFeaturesGenerator(true, project, className).generate();
    }

    @ActionTitle("генерирует smoke тесты верстки для DEV")
    public void generateSmokeLayoutTestsDev(final String project) {
        new SmokeLayoutFeaturesGenerator(false, project).generate();
    }

    @ActionTitle("генерирует smoke тесты верстки для DEV")
    public void generateSmokeLayoutTestsDev(final String project, final String className) {
        new SmokeLayoutFeaturesGenerator(false, project, className).generate();
    }
}
