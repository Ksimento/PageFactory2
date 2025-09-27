package ru.sbt.edu_power.e2e_core.devtools.network;

import ru.sbt.edu_power.e2e_core.confluence_integration.UpdateRequestStatTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ConnectionStatRepository {
    private static final Map<String, ConnectionStat> CONNECTION_STAT_MAP = new ConcurrentHashMap<>();

    public static synchronized void put(final String id, final ConnectionStat connectionStat) {
        CONNECTION_STAT_MAP.put(id, connectionStat);
    }

    public static void putAll(final Map<String, ConnectionStat> statMap) {
        statMap.forEach(ConnectionStatRepository::put);
    }

    public static synchronized ConnectionStat get(final String id) {
        return CONNECTION_STAT_MAP.get(id);
    }

    public static boolean containsKey(final String id) {
        return CONNECTION_STAT_MAP.containsKey(id);
    }

    public static Map<String, Map<String, Integer>> getRequestStat() {
        final Map<String, List<ConnectionStat>> requestListByRequestName = new HashMap<>();
        CONNECTION_STAT_MAP.forEach((id, stat) -> {
            if (!requestListByRequestName.containsKey(stat.getRequestName())) {
                requestListByRequestName.put(stat.getRequestName(), new ArrayList<>());
            }
            requestListByRequestName.get(stat.getRequestName()).add(stat);
        });
        final Map<String, Map<String, Integer>> requestStats = new LinkedHashMap<>();
        requestListByRequestName.forEach((name, list) -> {
            final Map<String, Integer> stat = new HashMap<>();
            stat.put(
                    UpdateRequestStatTable.Column.ALL_REQUESTS.getColName(),
                    list.size()
            );
            final Optional<Integer> min = list.stream()
                                           .map(ConnectionStat::getDuration)
                                           .filter(d -> d != 0)
                                           .min(Integer::compare);
            stat.put(
                    UpdateRequestStatTable.Column.FASTEST.getColName(),
                    min.orElse(0)
            );
            final Optional<Integer> max = list.stream()
                                           .map(ConnectionStat::getDuration)
                                           .filter(d -> d != 0)
                                           .max(Integer::compare);
            stat.put(
                    UpdateRequestStatTable.Column.SLOWEST.getColName(),
                    max.orElse(0)
            );
            final int failed = (int) list.stream()
                                    .map(ConnectionStat::isFailed)
                                    .filter(b -> b)
                                    .count();
            stat.put(
                    UpdateRequestStatTable.Column.ERRORS.getColName(),
                    failed
            );
            final int average = list.isEmpty() ? 0 : list.stream()
                    .map(ConnectionStat::getDuration)
                    .reduce(Integer::sum)
                    .orElse(0) / list.size();
            stat.put(
                    UpdateRequestStatTable.Column.AVERAGE.getColName(),
                    average
            );
            final int deviation = (max.orElse(0) * 100 / average) - 100;
            stat.put(
                    UpdateRequestStatTable.Column.DEVIATION.getColName(),
                    deviation
            );
            requestStats.put(name, stat);
        });
        return requestStats;
    }

    public static String getFormattedRequestStats() {
        final Map<String, Map<String, Integer>> map = getRequestStat();
        return map.keySet().stream()
                  .map(name -> {
                      final String formatted = map.get(name).keySet().stream()
                                                  .map(k -> String.format("\t\t%s: %s", k, map.get(name).get(k)))
                                                  .collect(Collectors.joining("\n"));
                      return name + "\n" + formatted;
                  })
                  .collect(Collectors.joining("\n\n"));
    }
}
