package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.sbt.edu_power.confluence_reporting.TableTune;
import ru.sbt.edu_power.external_services.confluence.ConfluenceConnectException;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Класс добавляет в таблицу данные по нарушению метрик и раскрашивает строки
 */
@Slf4j
public class ApplyMetrics {
    private final Map<TableHeaderInterface, Map<String, Integer>> tableData;
    private final TableTune tableTune;

    public ApplyMetrics(
            final Document document,
            final Map<TableHeaderInterface, Map<String, Integer>> tableData
    ) {
        this.tableData = tableData;
        tableTune = new TableTune(document);
    }

    @SneakyThrows
    public void apply(final AbstractMetrics metric, final TableHeaderInterface header) {
        log.info("Добавляются метрики для столбца {}", header.getColName());
        tableData.get(header).forEach((team, size) -> {
            if (metric.predicate().apply(size)) {
                final Element row = getRowByTeam(team);
                tableTune.markRow(row, metric.getBgColor());
                addDescription(row, metric.descriptionFunction().apply(team, size));
            }
        });
        log.info("Добавлены метрики для столбца {}", header.getColName());
    }

    @SneakyThrows
    public void applyNeedAutomate(final AbstractMetrics metric, final TableHeaderInterface header) {
        log.info("Добавляются метрики для столбца {}", header.getColName());
        tableData.get(header).forEach((team, size) -> {
            if (metric.predicateNeedAutomate().apply(size)) {
                final Element row = getRowByTeam(team);
                tableTune.markRow(row, metric.getBgColor());
                addDescription(row, metric.descriptionFunction().apply(team, size));
            }
        });
        log.info("Добавлены метрики для столбца {}", header.getColName());
    }

    private Element getRowByTeam(final String team) {
        return tableTune
                .getTableRowByText(team)
                .orElseThrow(() -> new ConfluenceConnectException("Не найдена строка с командой " + team));
    }

    private void addDescription(final Element row, final String description) {
        final Element cell = row.select("td").last();
        final Element p = new Element("p");
        if (description.contains("\n")) {
            Stream.of(description.split("\n"))
                  .forEach(t -> {
                      final Element e = new Element("p");
                      e.text(t);
                      p.appendChild(e);
                  });

        } else {
            p.text(description);
        }
        cell.insertChildren(0, p);
    }
}
