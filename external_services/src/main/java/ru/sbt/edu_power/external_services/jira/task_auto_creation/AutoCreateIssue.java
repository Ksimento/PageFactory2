package ru.sbt.edu_power.external_services.jira.task_auto_creation;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraQueueExecutorRunner;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.Issue;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssuePriority;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueProject;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;
import ru.sbt.edu_power.external_services.jira.tc_verifier.JiraTestCaseCollector;
import ru.sbt.edu_power.external_services.jira.test_manager.TMFields;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Класс реализует автоматическое создание задач на автоматизаторов
 * по результатам ночного прогона
 */
@Slf4j
public class AutoCreateIssue {
    private final String testCaseKey;
    private final IssueProject project;
    private IssuePriority priority = IssuePriority.MEDIUM;
    private String description = "";
    private static final String SUMMARY = "[NB AUTO CREATE] Разобрать упавшие тесты в ночном прогоне";
    private static final String ALLURE_LINK = "https://jenkins3-dev.pcbltools.ru/job/EduPower/job/QA/job/qa-java-ui-auto/" +
                                              System.getProperty("target.directory") +
                                              "/allure/";
    // Функция выполняет прикрепление issue к тест-кейсу
    private static final Consumer<Map<String, ?>> ADD_ISSUE_LINKS = (data) -> {
        final List<String> testCaseSet = new ArrayList<>(data.keySet());
        for (final String testCase : testCaseSet) {
            final String key = JiraQueueExecutorRunner.decodeTag(testCase);
            final String issue = searchOpenIssueByLastNightBuild(key);

            // если кейс ещё не создан - пропускаем итерацию
            if (issue == null) {
                continue;
            }
            // Если к задаче ночного прогона уже прилинковано 10 тест-кейсов - не линкуем все остальные кейсы
            // считаем, что произошло массовое падение
            final EnumMap<TMFields, String> query = new EnumMap<>(TMFields.class);
            query.put(TMFields.ISSUE_LINKS, issue);
            final List<String> linkedTestCase = JiraTestCaseCollector.getInstanceUI().filterCase(query);
            if (linkedTestCase.size() > 9) {
                data.remove(testCase);
                continue;
            }
            final List<String> issueLinks = JiraTestCaseCollector
                    .getInstanceUI()
                    .getFieldValueList(key, TMFields.ISSUE_LINKS);
            issueLinks.add(issue);
            final Map<String, List<String>> update = new HashMap<>();
            update.put(TMFields.ISSUE_LINKS.getFieldName(), issueLinks);
            try {
                JiraConnect.testCaseUpdate(key, update);
                JiraTestCaseCollector
                        .getInstanceUI()
                        .updateFieldMap(
                                TMFields.ISSUE_LINKS,
                                key,
                                String.join(
                                        TMFields.ISSUE_LINKS.getSeparator(),
                                        issueLinks
                                )
                        );
                data.remove(testCase);
            } catch (final Throwable e) {
                log.info("При отправке отчёта {} 'ADD_ISSUE_LINKS' возникла ошибка: {}",key, e.toString());
            }
        }
    };

    // функция создаёт новый issue
    private static final Consumer<Map<String, ?>> ISSUE_CREATE = (data) -> {
        final List<String> testCaseSet = new ArrayList<>(data.keySet());
        for (final String testCase : testCaseSet) {
            final String key = JiraQueueExecutorRunner.decodeTag(testCase);
            if (searchOpenIssueByLastNightBuild(key) != null) {
                data.remove(testCase);
                continue;
            }
            try {
                JiraConnect.jiraIssueCreate((Issue) data.get(testCase));
                data.remove(testCase);
            } catch (final Throwable e) {
                log.info("При отправке отчёта {} 'ISSUE_CREATE' возникла ошибка: {}", key, e.toString());
            }
        }
    };

    public AutoCreateIssue(final String testCaseKey) {
        this.testCaseKey = testCaseKey;
        project = IssueProject.valueOf(testCaseKey.split("-")[0]);
    }

    public void createIssue() {
        // если есть открытые ишью с прикрепленным кейсом - ничего дальше не делаем
        if (checkIfOpenIssueExist()) {
            return;
        }
        // Если не указан автоматизатор - пишем в текст грозное уведомление о недопустимости
        if (getAutomateEngineer(testCaseKey).isEmpty()) {
            priority = IssuePriority.HIGH;
            description = "Внимание! В перечисленных тест-кейсах не указан автоматизатор. Необходимо его указать";
        }
        description = String.join("\n", new String[]{ALLURE_LINK, description});
        final IssueFields issueFields = new IssueFields(
                project,
                IssueType.DEVTASK,
                priority,
                getTeam(testCaseKey),
                SUMMARY,
                description
        );
        // Добавляем в очередь создание новой задачи
        issueFields
                .setField(IssueFields.Field.ASSIGNEE, getAutomateEngineer(testCaseKey))
                .setField(IssueFields.Field.FIX_VERSIONS, "no version");
        final Map<String, Object> issueUpdate = new HashMap<>();
        issueUpdate.put(testCaseKey, new Issue(issueFields));
        JiraQueueExecutorRunner.getInstance().putToQueue(ISSUE_CREATE, issueUpdate);

        // Добавляем в очередь прикрепление тест-кейса к задаче
        final Map<String, Object> update = new HashMap<>();
        update.put(testCaseKey, "");
        JiraQueueExecutorRunner.getInstance().putToQueue(ADD_ISSUE_LINKS, update);
    }

    // Метод выполняет проверку, есть ли открытые ишью с прикрепленным кейсом
    private boolean checkIfOpenIssueExist() {
        final String[] issueLinks = JiraTestCaseCollector
                .getInstanceUI()
                .getFieldValueList(testCaseKey, TMFields.ISSUE_LINKS)
                .toArray(new String[]{});
        if (issueLinks.length == 0) {
            return false;
        }
        final IssueQuery query = new IssueQuery(0, 10);
        query.and(IssueFields.Field.PROJECT.getFieldName(), IssueQuery.Op.EQUAL, project.name())
             .and(IssueFields.Field.RESOLUTION.getFieldName(), IssueQuery.Op.EQUAL, "Unresolved")
             .and(IssueFields.Field.ISSUE_TYPE.getFieldName(), IssueQuery.Op.IN, IssueType.DEVTASK.getValue(), IssueType.BUG.getValue())
             .and("key", IssueQuery.Op.IN, issueLinks);
        final JiraSearch search = new JiraSearch(query);
        search.search();
        return search.getSearchResults() > 0;
    }

    // Метод ищет последний открытый ишью на инженера по текущему ночному прогону
    private static String searchOpenIssueByLastNightBuild(final String testCaseKey) {
        final IssueQuery query = new IssueQuery(0, 10);
        query.and(IssueFields.Field.SUMMARY.getFieldName(), IssueQuery.Op.CONTAIN, "NB AUTO CREATE")
             .and(IssueFields.Field.RESOLUTION.getFieldName(), IssueQuery.Op.EQUAL, "Unresolved")
             .and(IssueFields.Field.CREATED.getFieldName(), IssueQuery.Op.MORE_OR_EQUALS, "-120m")
             .and(IssueFields.Field.TEAM.name().toLowerCase(), IssueQuery.Op.EQUAL, getTeam(testCaseKey))
             .and(IssueFields.Field.ASSIGNEE.getFieldName(), IssueQuery.Op.EQUAL, getAutomateEngineer(testCaseKey));
        final JiraSearch search = new JiraSearch(query);
        search.search();
        return search.getSearchResults() > 0 ? search.getIssueList().keySet().iterator().next() : null;
    }

    // Метод получает возвращает автоматизатора по ID тест-кейса
    private static String getAutomateEngineer(final String testCaseKey) {
        return JiraTestCaseCollector.getInstanceUI().getFieldValue(testCaseKey, TMFields.AUTOMATOR);
    }

    private static String getTeam(final String testCaseKey) {
        return JiraTestCaseCollector.getInstanceUI().getFieldValue(testCaseKey, TMFields.TEAM);
    }
}
