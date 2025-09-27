package ru.sbt.edu_power.confluence_reporting.RTM_metrics;

import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Класс выполняет рассчёты метрик по данным из РТМ
 */
public class RtmMetricsCalc {
    private final RtmCollector rtmCollector;

    public RtmMetricsCalc(final RtmCollector rtmCollector) {
        this.rtmCollector = rtmCollector;
    }

    // Считаем сколько всего кейсов по командам
    public Map<String, Integer> caseAllMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> map.put(team, list.size()));
        return map;
    }

    // Считаем сколько всего тестов автоматизировано по командам (по статусу Автоматизирован - Да)
    public Map<String, Integer> automatedAllMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                .toMap()
                                .size()
                )
        );
        return map;
    }


    // Считаем сколько кейсов в статусе На автоматизации
    public Map<String, Integer> onAutomateMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.ON_AUTOMATE)
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем сколько всего ручных тестов
    public Map<String, Integer> handleTestsMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        list.size() -
                        TCFilter.of(list)
                                .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем сколько всего тестов по отчетности
    public Map<String, Integer> reportingTestsMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final int count = (int) list.values().stream()
                    .map(TestCaseModel::getLabels)
                    .filter(l -> l.contains("Отчетность") || l.contains("отчетность"))
                    .count();
            map.put(team, count);
        });
        return map;
    }

    // Считаем сколько всего критичных тестов по отчетности сейчас не используется в подсчете
    public Map<String, Integer> reportingCriticalTestsMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final int count = (int) list.values().stream()
                    .filter(l -> l.getLabels().contains("Отчетность") || l.getLabels().contains("отчетность"))
                    .filter(t ->t.getPriority() != null &&
                            t.getPriority().getName().equals(TCFields.Priority.HIGH.value))
                    .count();
            map.put(team, count);
        });
        return map;
    }

    // Считаем сколько кейсов исключено из прогона
    public Map<String, Integer> skippedMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.EXCLUDED)
                                .toMap()
                                .size()
                        +
                        TCFilter.of(list)
                                .filter(t -> t.getFramework() != null &&
                                             t.getFramework().contains(TCFields.Framework.JS))
                                .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.ON_AUTOMATE)
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем сколько тестов не подлежит автоматизации
    public Map<String, Integer> dontAutomateMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.NOT_REQUIRED)
                                .filter(t -> t.getNotAutomatedReason() != null && !t.getNotAutomatedReason().isEmpty())
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем сколько тестов не подлежит автоматизации из критичных
    public Map<String, Integer> criticalDontAutomateMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.NOT_REQUIRED)
                                .filter(t -> t.getNotAutomatedReason() != null && !t.getNotAutomatedReason().isEmpty())
                                .filter(t -> t.getPriority().getName().equals(TCFields.Priority.HIGH.value))
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем сколько тест-кейсов с полем Framework JS
    public Map<String, Integer> frameworkJs() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getFramework() != null &&
                                             t.getFramework().contains(TCFields.Framework.JS))
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем сколько тест-кейсов с полем Framework SELENIUM
    public Map<String, Integer> frameworkSelenium() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getFramework() != null &&
                                             t.getFramework().contains(TCFields.Framework.SELENIUM))
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем сколько всего критичных кейсов по командам
    public Map<String, Integer> criticalCaseMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getPriority() != null &&
                                             t.getPriority().getName().equals(TCFields.Priority.HIGH.value))
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    public Map<String, Integer> isKpiComplete(
            final Map<String, Integer> automatePercentByTeam,
            final Integer minAutomatePercent
    ) {
        final Map<String, Integer> map = new HashMap<>();
        automatePercentByTeam.forEach((team, percent) ->
                map.put(team, percent < minAutomatePercent ? 0 : 1)
        );
        return map;
    }

    // Считаем сколько автоматизировано только критичных кейсов по командам
    public Map<String, Integer> automatedCriticalCaseMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                .filter(t -> t.getPriority() != null &&
                                             t.getPriority().getName().equals(TCFields.Priority.HIGH.value))
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем сколько всего API тестов по командам
    public Map<String, Integer> caseAllApiMap() {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) ->
                map.put(
                        team,
                        TCFilter.of(list)
                                .filter(t -> t.getTestView() == TCFields.TestView.API)
                                .toMap()
                                .size()
                )
        );
        return map;
    }

    // Считаем процент автоматизации - ИСКЛЮЧАЯ не подлежащие автоматизации
    public Map<String, Integer> automatedPercentMapExclDontAut(final boolean onlyCriticalCases) {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final Map<String, TestCaseModel> actualList;
            if (onlyCriticalCases) {
                actualList = TCFilter.of(list)
                                     .filter(t -> t.getPriority() != null &&
                                                  t.getPriority().getName().equals(TCFields.Priority.HIGH.value))
                                     .toMap();
            } else {
                actualList = list;
            }
            if (actualList.isEmpty()) {
                map.put(team, 0);
                return;
            }
            final int excluded = TCFilter.of(actualList)
                                         .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.NOT_REQUIRED)
                                         .filter(t -> t.getNotAutomatedReason() != null &&
                                                      !t.getNotAutomatedReason().isEmpty())
                                         .toMap()
                                         .size();
            final int automated = TCFilter.of(actualList)
                                          .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                          .toMap()
                                          .size();
            final int percent = excluded == actualList.size() ? 0 : automated * 100 / (actualList.size() - excluded);
            map.put(team, percent);
        });
        return map;
    }

    // Процент автоматизации - ВКЛЮЧАЯ не подлежащие автоматизации
    public Map<String, Integer> automatedPercentMap(final boolean onlyCriticalCases) {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final Map<String, TestCaseModel> actualList;
            if (onlyCriticalCases) {
                actualList = TCFilter.of(list)
                                     .filter(t -> t.getPriority() != null &&
                                                  t.getPriority().getName().equals(TCFields.Priority.HIGH.value))
                                     .toMap();
            } else {
                actualList = list;
            }
            final int automated = TCFilter.of(actualList)
                                          .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                          .toMap()
                                          .size();
            final int percent = actualList.isEmpty() ? 0 : automated * 100 / actualList.size();
            map.put(team, percent);
        });
        return map;
    }

    // Количество тестов, которые нужно автоматизировать что бы достичь нужного KPI
    public Map<String, Integer> needAutomateForKpi(final int kpi) {
        final Map<String, Integer> map = new HashMap<>();

        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            if (kpi == 0) {
                map.put(team, 0);
                return;
            }
            final int allCriticalCases = (int) list.values()
                                                   .stream()
                                                   .filter(t -> t
                                                           .getPriority()
                                                           .getName()
                                                           .equals(TCFields.Priority.HIGH.value))
                                                   .filter(t -> !(t.getAutomatedStatus() ==
                                                                  TCFields.AutomatedStatus.NOT_REQUIRED &&
                                                                  (Objects.nonNull(t.getNotAutomatedReason()) &&
                                                                   !t.getNotAutomatedReason().isEmpty())
                                                           )
                                                   )
                                                   .count();
            final int automatedCriticalCases = (int) list.values()
                    .stream()
                    .filter(t -> t.getPriority().getName().equals(TCFields.Priority.HIGH.value))
                    .filter(t -> t.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                    .count();
            final int need = (allCriticalCases * (kpi + 1) / 100) - automatedCriticalCases;
            int percentOfCoverage = 0;
            if (allCriticalCases > 0){
                percentOfCoverage = (automatedCriticalCases * 100) / allCriticalCases;
            }
            if (percentOfCoverage < kpi && percentOfCoverage > 0 && need == 0){
                map.put(team, 1);
            } else {
                map.put(team, Math.max(0, need));
            }
        });
        return map;
    }

    // Собираем информацию по владельцам кейсов и автоматизаторам (список владельцев с количеством кейсов, список автоматизаторов с количеством кейсов)
    public Map<String, List<String>> testCaseOwners() {
        final Map<String, List<String>> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final Map<String, List<String>> owners = new HashMap<>();
            final Map<String, List<String>> aftOwner = new HashMap<>();
            list.forEach((k, t) -> {
                if (!owners.containsKey(t.getOwner())) {
                    owners.put(t.getOwner(), new ArrayList<>());
                }
                owners.get(t.getOwner()).add(k);

                if (
                        t.getAutomatedStatus() == TCFields.AutomatedStatus.YES ||
                        t.getAutomatedStatus() == TCFields.AutomatedStatus.EXCLUDED ||
                        t.getAutomatedStatus() == TCFields.AutomatedStatus.ON_AUTOMATE
                ) {
                    if (!aftOwner.containsKey(t.getAutomator())) {
                        aftOwner.put(t.getAutomator(), new ArrayList<>());
                    }
                    aftOwner.get(t.getAutomator()).add(k);
                }
            });
            final List<String> data = new ArrayList<>();
            data.add("Тест-кейсы:\n");
            data.addAll(owners.keySet()
                              .stream()
                              .map(o -> decodeName(o) + ": " + owners.get(o).size())
                              .collect(Collectors.toList()));
            data.add("\n \nАвтотесты:\n");
            data.addAll(aftOwner.keySet()
                                .stream()
                                .map(o -> decodeName(o) + ": " + aftOwner.get(o).size())
                                .collect(Collectors.toList()));
            map.put(
                    team,
                    data
            );
        });
        return map;
    }

    // Распределение тест-кейсов в соответствии с риском
    public Map<String, List<String>> testCaseRisk() {
        final Map<String, List<String>> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final EnumMap<TCFields.Risk, LongAdder> riskAccumMap = new EnumMap<>(TCFields.Risk.class);
            list.values().forEach(tc -> {
                if (Objects.isNull(tc.getRisk())) {
                    return;
                }
                if (!riskAccumMap.containsKey(tc.getRisk())) {
                    riskAccumMap.put(tc.getRisk(), new LongAdder());
                }
                riskAccumMap.get(tc.getRisk()).increment();
            });
            final int total = riskAccumMap.values()
                                          .stream()
                                          .map(LongAdder::intValue)
                                          .reduce(Integer::sum)
                                          .orElse(0);
            final List<String> data = new ArrayList<>();
            if (total != 0) {
                for (final TCFields.Risk risk : TCFields.Risk.values()) {
                    if (risk == TCFields.Risk.UNDEFINED) {
                        continue;
                    }
                    final int riskAccum = Objects.isNull(riskAccumMap.get(risk)) ? 0 : riskAccumMap
                            .get(risk)
                            .intValue();
                    final int percent = riskAccum * 100 / total;
                    final String line = String.format("%s (%d / %d%%)", risk.value, riskAccum, percent);
                    data.add(line);
                }
            }
            map.put(team, data);
        });
        return map;
    }

    /**
     * формулы для рассчёта данных по V3 - V4
     */

    // считаем количество тестов по лейблу
    public Map<String, Integer> getTestNumberByLabelFunction(final Function<TestCaseModel, Boolean> labelCheckFunction) {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final int total = (int) list.values()
                                        .stream()
                                        .filter(labelCheckFunction::apply)
                                        .count();
            map.put(team, total);
        });
        return map;
    }

    // считаем количество автоматизированных тестов по лейблу
    public Map<String, Integer> getAutomatedTestNumberByLabelFunction(final Function<TestCaseModel, Boolean> labelCheckFunction) {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final int automated = (int) list.values()
                                            .stream()
                                            .filter(labelCheckFunction::apply)
                                            .filter(tc -> tc.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                            .count();
            map.put(team, automated);
        });
        return map;
    }

    // считаем количество критичных тестов по лейблу
    public Map<String, Integer> getCriticalTestNumberByLabelFunction(
            final Function<TestCaseModel, Boolean> labelCheckFunction
    ) {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final int critical = (int) list.values()
                                           .stream()
                                           .filter(labelCheckFunction::apply)
                                           .filter(tc -> tc
                                                   .getPriority()
                                                   .getName()
                                                   .equals(TCFields.Priority.HIGH.value))
                                           .count();
            map.put(team, critical);
        });
        return map;
    }

    // считаем количество критичных автоматизированных тестов по лейблу
    public Map<String, Integer> getCriticalAutomatedTestNumberByLabelFunction(
            final Function<TestCaseModel, Boolean> labelCheckFunction
    ) {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final int critAutomated = (int) list.values()
                                                .stream()
                                                .filter(labelCheckFunction::apply)
                                                .filter(tc -> tc
                                                        .getPriority()
                                                        .getName()
                                                        .equals(TCFields.Priority.HIGH.value))
                                                .filter(tc -> tc.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                                .count();
            map.put(team, critAutomated);
        });
        return map;
    }

    // считаем процент критичных автоматизированных тестов по лейблу
    public Map<String, Integer> getCriticalAutomatedPercentTestNumberByLabelFunction(
            final Function<TestCaseModel, Boolean> labelCheckFunction
    ) {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final List<TestCaseModel> total = list.values()
                                                  .stream()
                                                  .filter(labelCheckFunction::apply)
                                                  .filter(tc -> tc
                                                          .getPriority()
                                                          .getName()
                                                          .equals(TCFields.Priority.HIGH.value))
                                                  .collect(Collectors.toList());

            final int critAutomated = (int) total.stream()
                                                 .filter(tc -> tc.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                                 .count();
            final int percent = total.isEmpty() ? 0 : critAutomated * 100 / total.size();
            map.put(team, percent);
        });
        return map;
    }

    // считаем процент критичных автоматизированных тестов исключая не автоматизируемые по лейблу
    public Map<String, Integer> getCriticalAutomatedPercentExcludeDontAutomateTestNumberByLabelFunction(
            final Function<TestCaseModel, Boolean> labelCheckFunction
    ) {
        final Map<String, Integer> map = new HashMap<>();
        rtmCollector.getUiCaseListByTeam().forEach((team, list) -> {
            final Map<String, TestCaseModel> total = TCFilter.of(list)
                                                             .filter(labelCheckFunction)
                                                             .filter(tc -> tc
                                                                     .getPriority()
                                                                     .getName()
                                                                     .equals(TCFields.Priority.HIGH.value))
                                                             .filter(this::isNotExcludedTestCase)
                                                             .toMap();

            final int critAutomated = TCFilter.of(total)
                                              .filter(tc -> tc.getAutomatedStatus() == TCFields.AutomatedStatus.YES)
                                              .toMap()
                                              .size();
            final int percent = total.isEmpty() ? 0 : critAutomated * 100 / total.size();
            map.put(team, percent);
        });
        return map;
    }

    // получаю количество всех тест-кейсов, исключая не подлежащих автоматизации
    // по заданному лейблу
    public int getTestNumberExcludeDontAutomateByLabelFunction(final Function<TestCaseModel, Boolean> labelCheckFunction) {
        return (int) rtmCollector.getUiCaseListByTeam().values()
                                 .stream()
                                 .map(Map::values)
                                 .flatMap(Collection::stream)
                                 .filter(labelCheckFunction::apply)
                                 .filter(tc -> tc.getPriority().getName().equals(TCFields.Priority.HIGH.value))
                                 .filter(this::isNotExcludedTestCase)
                                 .count();
    }

    private boolean isNotExcludedTestCase(final TestCaseModel tc) {
        final boolean isExcluded = tc.getAutomatedStatus() == TCFields.AutomatedStatus.NOT_REQUIRED
                                   && Objects.nonNull(tc.getNotAutomatedReason())
                                   && !tc.getNotAutomatedReason().isEmpty();
        return !isExcluded;
    }

    private String decodeName(final String login) {
        return ConfluenceDocument.getUserDisplayName(login);

    }

    public RtmCollector getRtmCollector() {
        return rtmCollector;
    }

}
