package ru.sbt.edu_power.test_duration_report;

import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteGrabber;
import ru.sbt.edu_power.external_services.jenkins.allure.TestType;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;

import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class TestDurationTable extends ConfluenceDocument {
    public static final String SKIP_LONG_TEST_TAG = "long_test";
    private final String buildUrl;
    private final String projectKey;
    private final SuiteCollector suiteCollector = new SuiteCollector();
    private final EnumMap<TableHeader, Map<String, Long>> numberData = new EnumMap<>(TableHeader.class);
    private final EnumMap<TableHeader, Map<String, String>> stringData = new EnumMap<>(TableHeader.class);
    private static final long JAVA_UI_SUCCESS = (4 * 60 + 1) * 1000;
    private static final long JAVA_UI_FAILED = (long) (4.4 * 60 * 1000);
    private static final long JS_SUCCESS = 61 * 1000;
    private static final long JS_FAILED = (long) ((1.5 * 60 + 1) * 1000);
    private static final long API_SUCCESS = 41 * 1000;
    private static final long API_FAILED = 61 * 1000;

    public TestDurationTable(final String buildUrl, final String projectKey, final String pageId) {
        super(pageId);
        this.buildUrl = buildUrl.split("allure")[0];
        this.projectKey = projectKey;
    }

    public void execute() {
        loadDocument();
        removeTableData();
        removePanelData();
        new SuiteGrabber().grab(suiteCollector, buildUrl);
        addHeaders();
        sortData();
        addStatistic();
        insertHeaderToDocument(
                Stream.of(TableHeader.values())
                        .collect(Collectors.toList()), this::formatHeader
        );
        insertDataToTable();
        saveDocument();
    }

    private void addStatistic() {
        final long[] durations = suiteCollector.getCollection().values().stream()
                .flatMap(List::stream)
                .filter(ch -> ch.getTags().stream().map(t -> t.replace("@", "")).noneMatch(SKIP_LONG_TEST_TAG::equals))
                .map(ch -> ch.getReport().getTime().getDuration())
                .mapToLong(Long::longValue)
                .toArray();

        final long longTestSkipped = suiteCollector.getCollection().values().stream()
                .flatMap(List::stream)
                .filter(ch -> ch.getTags().stream().map(t -> t.replace("@", "")).anyMatch(SKIP_LONG_TEST_TAG::equals))
                .count();

        if (durations.length > 0) {
            Arrays.sort(durations);
            final long max = durations[durations.length - 1];
            final long sum = LongStream.of(durations).sum();
            final long average = sum / durations.length;
            final long median = durations.length % 2 == 0 ?
                    (durations[durations.length / 2] + durations[durations.length / 2 - 1]) / 2L :
                    durations[durations.length / 2];
            insertDataToPanel(String.format(
                    "<div><b>Всего обработано тестов:</b> %d</div>",
                    suiteCollector.getCollection().values().stream().mapToInt(Collection::size).sum())
            );
            insertDataToPanel(String.format("<div><b>Самый длинный тест:</b> %s</div>", formatDuration(max)));
            insertDataToPanel(String.format("<div><b>Среднее время теста:</b> %s</div>", formatDuration(average)));
            insertDataToPanel(String.format("<div><b>Медианное время теста:</b> %s</div>", formatDuration(median)));
            insertDataToPanel(String.format("<div><b>Всего тестов с превышением времени:</b> %d</div>", numberData.get(TableHeader.DURATION).keySet().size()));
        }

        if (longTestSkipped > 0) {
            insertDataToPanel(String.format("<div><b>Пропущено длинных тестов:</b> %s</div>", longTestSkipped));
        }
    }

    private String formatHeader(final TableHeaderInterface tableHeader) {
        switch ((TableHeader) tableHeader) {
            case TEST_NAME:
                return formatHeader(tableHeader.getColName(), 150);
            case TAGS:
                return formatHeader(tableHeader.getColName(), 20);
            case TEAM:
                return formatHeader(tableHeader.getColName(), 80);
            default:
                return tableHeader.getColName();
        }
    }

    private void insertDataToTable() {
        final BiFunction<String, TableHeaderInterface, String> converterFunction = (row, header) -> {
            final String result;
            switch ((TableHeader) header) {
                case TEST_NAME:
                    result = row;
                    break;
                case DURATION:
                    result = formatDuration(numberData.get(header).get(row));
                    break;
                default:
                    result = stringData.get(header).get(row);
                    break;
            }
            return result;
        };

        final List<String> rowNames = new ArrayList<>(numberData.get(TableHeader.DURATION).keySet());
        rowNames.sort((o1, o2) -> {
            final String team1 = stringData.get(TableHeader.TEAM).get(o1);
            final String team2 = stringData.get(TableHeader.TEAM).get(o2);
            if (!team1.equals(team2)) {
                return team1.compareTo(team2);
            }
            return numberData.get(TableHeader.DURATION).get(o2).compareTo(numberData.get(TableHeader.DURATION).get(o1));
        });
        insertDataToTable(
                rowNames,
                Stream.of(TableHeader.values()).collect(Collectors.toList()),
                converterFunction
        );
    }


    private void addHeaders() {
        numberData.put(TableHeader.DURATION, new HashMap<>());
        stringData.put(TableHeader.TAGS, new HashMap<>());
        stringData.put(TableHeader.TEAM, new HashMap<>());
        stringData.put(TableHeader.TEST_TYPE, new HashMap<>());
    }

    private void sortData() {
        suiteCollector.getCollection().values().stream()
                .flatMap(List::stream)
                .forEach(ch -> {
                    if (ch.getTags().stream().map(t -> t.replace("@", "")).anyMatch(SKIP_LONG_TEST_TAG::equals)) {
                        return;
                    }
                    final TestType testType = TestType.determineByTags(ch.getTags());
                    final Long duration = ch.getReport().getTime().getDuration();
                    if ("passed".equals(ch.getStatus())) {
                        if (testType == TestType.JAVA_UI && duration <= JAVA_UI_SUCCESS) {
                            return;
                        } else if (testType == TestType.JS_UI && duration <= JS_SUCCESS) {
                            return;
                        } else if (testType == TestType.JAVA_API && duration <= API_SUCCESS) {
                            return;
                        }
                    } else {
                        if (testType == TestType.JAVA_UI && duration <= JAVA_UI_FAILED) {
                            return;
                        } else if (testType == TestType.JS_UI && duration <= JS_FAILED) {
                            return;
                        } else if (testType == TestType.JAVA_API && duration <= API_FAILED) {
                            return;
                        }
                    }
                    final List<String> tags = ch.searchTags(projectKey);
                    final String testName = String.format("<a href='%s' target='_blank'>%s</a>", ch.getLink(), ch.getName());
                    final String tagLinks = tags.stream()
                            .map(tag -> String.format("<a href='https://jira.pcbltools.ru/jira/secure/Tests.jspa#/testCase/%s' target='_blank'>%s</a>", tag, tag))
                            .collect(Collectors.joining(", "));
                    String team;
                    if (tags.isEmpty()) {
                        team = "Не определена";
                    } else {
                        try {
                            final String tcTeam = NewJiraTCCollector.getInstance().getById(tags.get(0)).getTeam();
                            team = Objects.isNull(tcTeam) || tcTeam.isEmpty() ? "Не определена" : tcTeam;
                        } catch (final Throwable e) {
                            team = "Не определена";
                        }
                    }
                    numberData.get(TableHeader.DURATION).put(testName, duration);
                    stringData.get(TableHeader.TAGS).put(testName, tagLinks);
                    stringData.get(TableHeader.TEAM).put(testName, team);
                    stringData.get(TableHeader.TEST_TYPE).put(testName, testType.name());
                });
    }

    private String formatDuration(final long durationMs) {
        final Duration duration = Duration.ofMillis(durationMs);
        if (duration.getSeconds() < 60) {
            return duration.getSeconds() + "s";
        }
        return String.format("%dm %ds", duration.toMinutes(), duration.getSeconds() % 60);
    }

    public enum TableHeader implements TableHeaderInterface {
        TEST_NAME("Название теста", false),
        TAGS("Тест-кейсы", false),
        DURATION("Длительность теста", false),
        TEST_TYPE("Тип теста", false),
        TEAM("Команда", false);

        private final String colName;
        private final boolean doNotRender;

        TableHeader(final String colName, final boolean doNotRender) {
            this.colName = colName;
            this.doNotRender = doNotRender;
        }

        @Override
        public String getColName() {
            return colName;
        }

        @Override
        public boolean doNotRender() {
            return doNotRender;
        }
    }
}
