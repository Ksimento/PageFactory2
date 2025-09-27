package ru.sbt.edu_power.test_manager.report_component_tests;


import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.sbt.edu_power.confluence_reporting.TableTune;
import ru.sbt.edu_power.external_services.confluence.ConfluenceConnect;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ReportPageComponent extends ConfluenceDocument {
    List<Element> header = new ArrayList<>();
    List<Mfe> mfeList = new ArrayList<>();
    Document tableDataMfe;

    protected ReportPageComponent(String pageID) {
        super(pageID);
    }

    protected void generateDocument(final List<ReportComponent> reportComponentList) {
        loadDocument();
        removeTableData();
        tableDataMfe = Jsoup.parse(ConfluenceConnect.getPage(
                                                            "84823351",
                                                            ConfluenceConnect.Expand.BODY_VIEW
                                                    ).getBody()
                                                    .getObject()
                                                    .getJSONObject("body")
                                                    .getJSONObject("view")
                                                    .getString("value"));
        header = tableDataMfe.selectXpath("//tr[1]//td");
        int sizeRow = tableDataMfe.selectXpath("//td[1]").size();
        int idRepos = getIndexHeader("Репозиторий");
        int idTeam = getIndexHeader("Команда");
        int idStatus = getIndexHeader("Статус MFE");
        for (int i = 1; i < sizeRow; i++) {
            Mfe mfe = new Mfe();
            mfe.setName(getDataTableInIndex(idRepos, i));
            mfe.setTeam(getDataTableInIndex(idTeam, i));
            mfe.setStatusMfe(getDataTableInIndex(idStatus, i));
            mfeList.add(mfe);
        }
        insertHeaderToDocument(Stream
                .of(Header.values())
                .collect(Collectors.toList()), this::formatHeader);
        getDocument().selectFirst("table").appendChild(new Element("tbody"));
        reportComponentList.forEach(this::addCells);
        log.info(getDocument().html());
        saveDocument();
    }

    private String formatHeader(final TableHeaderInterface header) {
        return header.getColName();
    }

    private String getDataTableInIndex(final int indexTd, final int indexTr) {
        return tableDataMfe.selectXpath("//td[" + indexTd + "]").get(indexTr).text();
    }

    private void addCells(final ReportComponent reportComponent) {
        updateTeam(reportComponent);
        log.info(reportComponent.getNameComponent() + " статус = " + reportComponent.isProject());
        Element tr = new Element("tr")
                .appendChild(new Element("td")
                        .appendChild((new Element("a"))
                                .attr("href", reportComponent.getUrlAllure())
                                .text(reportComponent.getNameComponent())))
                .appendChild(new Element("td").text(String.valueOf(reportComponent.getRtmAllTest())))
                .appendChild(new Element("td").text(String.valueOf(reportComponent.getAllTest())))
                .appendChild(new Element("td").text(String.valueOf(reportComponent.getPassedTest())))
                .appendChild(new Element("td").text(String.valueOf(reportComponent.getFailTest())))
                .appendChild(new Element("td").text(String.valueOf(reportComponent.getSkipTest())))
                .appendChild(new Element("td").text(String.valueOf(reportComponent.getTeam())))
                .appendChild(getStatusText(reportComponent))
                .appendChild(new Element("td").text(reportComponent.getStatusMfe()));
        getDocument()
                .selectFirst("tbody")
                .appendChild(setStatus(tr, reportComponent));

    }

    private void updateTeam(final ReportComponent reportComponent) {
        Mfe component = mfeList
                .stream()
                .filter(mfe -> mfe.name.equals(reportComponent.getNameComponent()))
                .findAny()
                .orElse(null);
        if (component != null) {
            reportComponent.setTeam(component.getTeam());
            reportComponent.setStatusMfe(component.getStatusMfe());
        }
    }

    private Element getStatusText(final ReportComponent reportComponent) {
        List<Element> elements = new ArrayList<>();
        if (!reportComponent.statusTests) {
            reportComponent.getPipeError().stream().forEach(s -> elements.add(new Element("p").text(s)));
        } else {
            if (reportComponent.getAllTest() < 1 || reportComponent.getRtmAllTest() < 1) {
                elements.add(new Element("p").text("Quality Gate не пройден"));
                elements.add(new Element("p").text(String.format("Запущено тестов %s", reportComponent.getAllTest())));
                elements.add(new Element("p").text(String.format("Тестов в РТМ %s", reportComponent.getRtmAllTest())));
                reportComponent.statusTests = false;
            } else {
                elements.add(new Element("p").text("Quality Gate пройден"));
            }
        }
        Element td = new Element("td");
        elements.forEach(td::appendChild);
        return td;
    }

    private int getIndexHeader(String nameHeader) {
        for (int i = 0; i < this.header.size(); ++i) {
            if (header.get(i).text().equals(nameHeader)) {
                return i+1;
            }
        }

        throw new IndexHeaderException("Не удалось найти заголовок");
    }


    private Element setStatus(final Element element, final ReportComponent reportComponent) {
        final boolean runStatus = reportComponent.statusTests;
        final boolean statusMfe = reportComponent.getStatusMfe().equals("Поддерживается");
        if (!runStatus || !statusMfe) {
            element.select("td").stream().forEach(e -> {
                if (!statusMfe) {
                    e.attr("data-highlight-colour", TableTune.BgColor.YELLOW.getDataHighlightColour());
                    e.attr("class", TableTune.BgColor.YELLOW.getClassAttr());
                } else {
                    e.attr("data-highlight-colour", TableTune.BgColor.RED.getDataHighlightColour());
                    e.attr("class", TableTune.BgColor.RED.getClassAttr());
                }
            });
        }
        return element;
    }


    public enum Header implements TableHeaderInterface {
        COMPONENT("Компонент"),
        RTM_TESTS("Тестов в РТМ"),
        ALL_TEST("Всего тестов"),
        PASSED("Успешных"),
        FAIL("Упавших"),
        SKIP("Пропущенных"),
        TEAM("Команда"),
        RUN_STATUS("Quality Gate"),
        STATUS_MFE("Статус MFE");

        public final String name;

        Header(final String name) {
            this.name = name;
        }

        @Override
        public String getColName() {
            return name;
        }

        @Override
        public boolean doNotRender() {
            return false;
        }
    }
}
