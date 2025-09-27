package ru.sbt.edu_power.smoke_report;

import org.jsoup.nodes.Element;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.AllureReport;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TableStatistic {

    private static Map<String, String> getFailDataInTable(final Map<String, List<Children>> collection) {
        Map<String, String> map = new HashMap<>();
        for (String team : collection.keySet()) {
            List<AllureReport.Step> teamSteps = getSteps(collection, team);
            List<AllureReport.Step> failRout =  teamSteps.stream()
                                                         .filter(s -> (!"passed".equals(s.getStatus()) && !"skipped".equals(s.getStatus()))).collect(
                            Collectors.toList());
            if (failRout.size() != 0) {
                AtomicReference<String> page = new AtomicReference<>("");
                failRout.forEach(e -> {
                             page.set(page + e.getName().split("\"")[1] + "; ");
                         });
                map.put(team, page.get());
            }
        }
        return map;
    }

    private static String getDataRoutUntagged(final Map<String, List<Children>> collection) {
        try {
            List<AllureReport.Step> teamSteps = getSteps(collection, "Untagged");
            if (!teamSteps.isEmpty()) {
                AtomicReference<String> page = new AtomicReference<>("");
                teamSteps.forEach(e -> {
                    page.set(page + e.getName().split("\"")[1] + "; ");
                });
                return page.get();
            }
        }
        catch (NullPointerException e){
            return "\uD83C\uDF86\uD83C\uDF86УРА!!Все смоки распределены по командам!!\uD83C\uDF86\uD83C\uDF86\uD83C\uDF86";
        }
        throw new SmokeReportError("Что то пошло не так при получении нераспределенных роутов");
    }

    public static String getTableFailRout(final Map<String, List<Children>> collection) {
        final Element table = new Element("table");
        table.attr("style", "border: 1px solid black;");
        final Element tbody = new Element("tbody");
        tbody.appendChild(writeDataTable("Команда", "Количество упавших страниц", "Упавшие пейджы"));
        final Map<String, String> dataTable = getFailDataInTable(collection);
        for (String team : dataTable.keySet()) {
            final String pages = dataTable.get(team);
            final int failPages = pages.split(";").length - 1;
            tbody.appendChild(writeDataTable(
                    team,
                    String.valueOf(pages.split(";").length - 1),
                    failPages > 5 ? "По команде более 5 падений, просмотр через аллюр" : pages
            ));
        }
        table.appendChild(tbody);
        return table.toString();
    }

    public static String getLineRoutUntagged(final Map<String, List<Children>> collection) {
        final String textRoutUntagged = getDataRoutUntagged(collection);
        return !textRoutUntagged.contains("УРА!") ? "<div>Не распределено: " +
                                                    (textRoutUntagged.split(";").length - 1) +
                                                    " экранов</div>" +
                                                    "<div>Список не распределенных пейджей: " +
                                                    textRoutUntagged +
                                                    "</div>" : "</div>" + textRoutUntagged + "</div>";
    }

    private static Element writeDataTable(final String column1, final String column2, final String column3) {
        final Element tr1 = new Element("tr");
        final Element tdLine1 = new Element("td");
        final Element tdLine2 = new Element("td");
        final Element tdLine3 = new Element("td");
        tdLine1.attr("style", "border: 1px solid black;");
        tdLine2.attr("style", "text-align: center; vertical-align: middle; border: 1px solid black;");
        tdLine3.attr("style", "border: 1px solid black;");
        final Element strongLine1 = new Element("strong");
        final Element strongLine2 = new Element("strong");
        final Element strongLine3 = new Element("strong");
        tdLine1.appendChild(strongLine1.text(column1));
        tdLine2.appendChild(strongLine2.text(column2));
        tdLine3.appendChild(strongLine3.text(column3));
        tr1.appendChild(tdLine1);
        tr1.appendChild(tdLine2);
        tr1.appendChild(tdLine3);
        return tr1.attr("style", "border: 1px solid black;background: #98FB98;");
    }

    private static List<AllureReport.Step> getSteps(final Map<String, List<Children>> collection, String team) {
        return collection.get(team).stream()
                         .map(Children::getReport)
                         .map(AllureReport::getTestStage)
                         .map(AllureReport.TestStage::getSteps)
                         .flatMap(Set::stream)
                         .filter(s -> s.getName().contains("роут"))
                         .collect(Collectors.toList());
    }
}