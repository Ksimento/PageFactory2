package ru.sbt.edu_power.confluence_reporting.RTM_metrics;

import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Класс выполняет сбор данных из РТМ, а так же подготавливает наборы данных для дальнейших рассчётов
 */
public class RtmCollector {
    private final TCQueryBuilder queryBuilder;
    // Данные по кейсам РТМ в разрезе команд
    // team -> test case list
    private final Map<String, Map<String, TestCaseModel>> uiCaseListByTeam = new TreeMap<>();
    private final NewJiraTCCollector collector = new NewJiraTCCollector();

    public RtmCollector(final TCQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        collectRTMData();
    }

    public Map<String, Map<String, TestCaseModel>> getUiCaseListByTeam() {
        return uiCaseListByTeam;
    }

    // загружает данные из РТМ выбранной проектной области и распределяет их по признакам (автоматизирован, фреймворк, команда)
    private void collectRTMData() {
        collector.collect(queryBuilder);
        TCFilter.of(collector.asMap())
                .toMap()
                .values()
                .forEach(t -> {
                            final String team = t.getTeam();
                            if (team == null || team.trim().isEmpty()) {
                                return;
                            }
                            if (!uiCaseListByTeam.containsKey(team)) {
                                uiCaseListByTeam.put(team, new HashMap<>());
                            }
                            uiCaseListByTeam.get(team).put(t.getKey(), t);
                        }
                );
    }

    public void alterTestCaseMap(final Consumer<Map<String, Map<String, TestCaseModel>>> alterDataFunction) {
        alterDataFunction.accept(uiCaseListByTeam);
    }

    public NewJiraTCCollector getCollector() {
        return collector;
    }
}
