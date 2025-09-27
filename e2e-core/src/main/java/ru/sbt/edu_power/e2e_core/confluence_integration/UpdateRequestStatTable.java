package ru.sbt.edu_power.e2e_core.confluence_integration;

import org.jsoup.nodes.Element;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Класс обновляет страницу конфлюенса со статистикой времени выполнения запросов
 */
public class UpdateRequestStatTable extends ConfluenceDocument {
    private final Map<String, Integer> columns = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> data;
    private final Map<String, Map<String, Integer>> table = new HashMap<>();

    public UpdateRequestStatTable(final String pageID, final Map<String, Map<String, Integer>> data) {
        super(pageID);
        this.data = data;
    }

    public void updatePage() {
        loadDocument();
        collectColumns();
        if (checkIfCurrentDate()) {
            parseTable();
        }
        putDataToTable();
        updateTableData();
        saveDocument();
    }

    private void collectColumns() {
        int i = 0;
        for (final Column col : Column.values()) {
            columns.put(col.getColName(), i);
            i++;
        }
    }

    private void parseTable() {
        getDocument().select("tbody tr").forEach(row -> {
            final Map<String, Integer> stat = new HashMap<>();
            columns.forEach((c, n) -> {
                if (c.equals(Column.REQUEST.getColName())) {
                    table.put(row.select("td").get(n).text(), stat);
                } else {
                    stat.put(c, Integer.parseInt(row.select("td").get(n).text()));
                }
            });
        });
    }

    private void putDataToTable() {
        data.forEach((req, stat) -> {
            if (!table.containsKey(req)) {
                table.put(req, stat);
            } else {
                final Map<String, Integer> tableStat = table.get(req);
                final int allReqData = stat.get(Column.ALL_REQUESTS.getColName());
                final int allReqTable = tableStat.get(Column.ALL_REQUESTS.getColName());
                final int avrData = stat.get(Column.AVERAGE.getColName());
                final int avrTable = tableStat.get(Column.AVERAGE.getColName());
                final int average = (allReqData * avrData + allReqTable * avrTable) / (allReqData + allReqTable);
                final int errors = stat.get(Column.ERRORS.getColName()) + tableStat.get(Column.ERRORS.getColName());
                final int fastest = Math.min(stat.get(Column.FASTEST.getColName()), tableStat.get(Column.FASTEST.getColName()));
                final int slowest = Math.max(stat.get(Column.SLOWEST.getColName()), tableStat.get(Column.SLOWEST.getColName()));
                tableStat.put(Column.ALL_REQUESTS.getColName(), allReqData + allReqTable);
                tableStat.put(Column.AVERAGE.getColName(), average);
                tableStat.put(Column.ERRORS.getColName(), errors);
                tableStat.put(Column.FASTEST.getColName(), fastest);
                tableStat.put(Column.SLOWEST.getColName(), slowest);
                final int deviation = (slowest * 100 / average) - 100;
                tableStat.put(Column.DEVIATION.getColName(), deviation);
            }
        });
    }

    private boolean checkIfCurrentDate() {
        final Element element = getDocument().selectFirst("p");
        if (element == null || !element.text().equals(getCurrentDate())) {
            reInitPage();
            return false;
        }
        return true;
    }

    private void reInitPage() {
        final Element body = getDocument().selectFirst("body");
        body.children().remove();
        final Element date = new Element("p");
        date.text(getCurrentDate());
        body.appendChild(date);
        final Element table = new Element("table");
        final Element header = new Element("thead");
        final Element headerRow = new Element("tr");
        columns.keySet().forEach(k -> {
            final Element cell = new Element("th");
            cell.text(k);
            headerRow.appendChild(cell);
        });
        body.appendChild(table);
        table.appendChild(header);
        header.appendChild(headerRow);
        table.appendChild(new Element("tbody"));
    }

    private String getCurrentDate() {
        final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        return format.format(new Date());
    }

    private void updateTableData() {
        final Element tbody = getDocument().selectFirst("tbody");
        if (!tbody.children().isEmpty()) {
            tbody.children().remove();
        }
        table.forEach((req, stat) -> {
            final Element row = new Element("tr");
            columns.keySet().forEach(colName -> {
                final Element cell = new Element("td");
                if (colName.equals(Column.REQUEST.getColName())) {
                    cell.text(req);
                } else {
                    cell.text(stat.get(colName).toString());
                }
                row.appendChild(cell);
            });
            tbody.appendChild(row);
        });
    }

    public enum Column {
        REQUEST("Запрос"),
        ALL_REQUESTS("Всего запросов"),
        FASTEST("Самый быстрый"),
        SLOWEST("Самый медленный"),
        ERRORS("Ошибок"),
        AVERAGE("Среднее время"),
        DEVIATION("Процент отклонения максимального значения от среднего");

        private final String colName;

        Column(final String colName) {
            this.colName = colName;
        }

        public String getColName() {
            return colName;
        }
    }
}
