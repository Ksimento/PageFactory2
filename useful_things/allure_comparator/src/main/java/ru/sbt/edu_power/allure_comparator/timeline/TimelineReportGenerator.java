package ru.sbt.edu_power.allure_comparator.timeline;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.shared.fail_categories.IFailCategories;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TimelineReportGenerator {
    private final DefectsSpreadingTimeline defectsSpreadingTimeline;
    private final Document document;
    private final int rowSize = 12;

    @SneakyThrows
    public TimelineReportGenerator(final SuiteCollector actualCollector) {
        this.defectsSpreadingTimeline = new DefectsSpreadingTimeline(actualCollector);
        final String baseUrl = Objects
                .requireNonNull(this.getClass().getClassLoader().getResource("timeline.html")).toString();
        document = Jsoup.parse(
                this.getClass().getClassLoader().getResourceAsStream("timeline.html"),
                StandardCharsets.UTF_8.name(),
                baseUrl
        );
    }

    public void generate() {
        defectsSpreadingTimeline.sortData();
        defectsSpreadingTimeline.generateTimeline();
        final Element input = new Element("input");
        input.attr("type", "text")
             .attr("placeholder", "введи ID тест-кейса")
             .attr("id", "tag-filter");
        final Element button = new Element("input");
        button.attr("id", "tag-search")
              .attr("type", "button")
              .attr("value", "Найти")
              .attr("onclick", "tagSearch()");
        final Element searchContainer = new Element("div");
        searchContainer.addClass("search");
        searchContainer.appendChild(input)
                       .appendChild(button);
        final Element body = Objects.requireNonNull(document.selectFirst("body"));
        body.appendChild(searchContainer);
        defectsSpreadingTimeline.getTimelineMap().forEach((jobName, timeline) -> {
            final Element job = new Element("h2");
            job.text(jobName);
            job.attr("style", "margin-left: 2%");
            body.appendChild(job)
                .appendChild(getChartLegend(timeline, jobName)).appendChild(getTableFallsByTeams(timeline, jobName))
                .appendChild(
                        getScaleHtml(
                                timeline.getWorkloadScale(),
                                92.5d,
                                "wlb",
                                i -> String.format("rgb(%s, 0, 255)", i),
                                String::valueOf
                        )
                )
                .appendChild(
                        getScaleHtml(
                                timeline.getErrorScale(),
                                91d,
                                "elb",
                                i -> String.format("rgb(255, %s, %s)", 255 - i, 255 - i),
                                i -> i + "%"
                        )
                );

            final Element container = new Element("div");
            container.attr(
                    "style",
                    String.format(
                            "position: relative; height: %dpx",
                            timeline.getRowToElementMap().size() * rowSize + 100
                    )
            );
            timeline.getRowToElementMap().forEach((row, list) -> {
                list.forEach(e -> container.appendChild(getLink(e, row, jobName)));
            });
            body.appendChild(container);
        });
        body.appendChild(jsSearch());
        body.appendChild(jsHideCategories());
        body.appendChild(jsOnChangeCheckboxEvent());
    }

    private Element jsSearch() {
        final String js = "function tagSearch() {[...document.getElementsByClassName('active')].forEach(e => {" +
                          "e.classList.remove('active');});let t = document.getElementById('tag-filter').value.replace('@', '');" +
                          "let c = document.querySelectorAll('[tags~=' + t + ']');" +
                          "[...c].forEach(e => {e.classList.add('active');e.scrollIntoView();})}";
        final Element script = new Element("script");
        script.text(js);
        return script;
    }

    private Element jsHideCategories() {
        final String js = "function hideCategory(element) {let c = element.target.getAttribute('id');\n" +
                          "[...document.querySelectorAll('[cbdata=' + c + ']')].forEach(e => {\n" +
                          "if (element.target.checked === true) {e.style['display'] = 'block';} else {\n" +
                          "e.style['display'] = 'none';}})}";
        final Element script = new Element("script");
        script.text(js);
        return script;
    }

    private Element jsOnChangeCheckboxEvent() {
        final String js =
                "let onChangeFunc = function() {[...document.getElementsByClassName('category-filter')].forEach(e => {\n" +
                "e.addEventListener('change', hideCategory);})};onChangeFunc();";
        final Element script = new Element("script");
        script.text(js);
        return script;
    }

    private Element getScaleHtml(
            final Scale scale,
            final double width,
            final String cssClass,
            final Function<Integer, String> color,
            final Function<Integer, String> content
    ) {
        final int min = scale.getMin();
        final int size = scale.getMax() - min;
        final double k = size / 255d;
        final int steps = scale.getScaleStepsNumber();
        final List<Integer> scaleData = scale.getScaleData();
        final Element container = new Element("div");
        container.attr("style", "position: relative; height: 20px; margin-left: 1%");
        container.addClass(cssClass);
        final double blockWidth = width / steps;
        scaleData.forEach(e -> {
            final int preNormalized = (int) ((e - min) / k);
            final int normalized;
            if (preNormalized > 255) {
                normalized = 255;
            } else {
                normalized = Math.max(preNormalized, 0);
            }
            final Element coloredBlock = new Element("div");
            coloredBlock.attr(
                    "style",
                    String.format(
                            Locale.US,
                            "width: %.5f%%; background-color: %s",
                            blockWidth,
                            color.apply(normalized)
                    )
            );
            coloredBlock.text(content.apply(e));
            container.appendChild(coloredBlock);
        });
        return container;
    }

    @SneakyThrows
    public void saveHtml() {
        final Path reportPath = Paths.get(System.getProperty("user.dir"), "target", "diff-report");
        final Path reportFile = Paths.get(reportPath.toString(), "timeline.html");
        Files.createDirectories(reportPath);
        Files.write(reportFile, Parser.unescapeEntities(document.html(), true).getBytes(StandardCharsets.UTF_8));
    }

    private Element getLink(final TimelineElement timelineElement, final int row, final String jobName) {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
        final Element container = new Element("a");
        final double left = timelineElement.getRelativePosition();
        final String width = String.format(Locale.US, "%.2f%%", Math.max(timelineElement.getRelativeWidth(), 0.15));
        final int top = 50 + row * rowSize;
        container.attr("style", String.format(Locale.US, "top: %dpx; left: %.2f%%; width: %s", top, left, width))
                 .attr("href", timelineElement.getChildren().getLink())
                 .attr("tags", timelineElement.getTags())
                 .addClass("item");
        // полоска теста
        final Element testLine = new Element("div");
        testLine.addClass(timelineElement.getChildren().getStatus()).addClass("ch")
                .attr(
                        "title",
                        String.format(
                                "%s - %s%n   команда: %s",
                                dateFormatter.format(new Date(timelineElement.getStartTime())),
                                timeFormatter.format(new Date(timelineElement.getEndTime())), timelineElement.getTeam()
                        )
                );
        container.appendChild(testLine);
        timelineElement.getFailPoints().forEach(point -> {
            final String cbdata = point.getCategory().getName() + "-" + jobName;
            final Element failElement = new Element("div");
            failElement
                    .addClass("failPoint")
                    .addClass(point.getCategory().getShape().name().toLowerCase())
                    .attr(
                            "style",
                            String.format(
                                    Locale.US,
                                    "left: %.2f%%; background-color: %s",
                                    point.getPosition(),
                                    point.getCategory().getColor()
                            )
                    )
                    .attr(
                            "title",
                            timeFormatter.format(new Date(point.getTime())) + " команда: " + timelineElement.getTeam()
                    )
                    .attr("cbdata", cbdata);
            container.appendChild(failElement);
        });
        return container;
    }

    private Element getChartLegend(final Timeline timeline, final String jobName) {
        final Map<IFailCategories, AtomicInteger> categories = new HashMap<>();
        timeline.getElements()
                .stream()
                .map(TimelineElement::getFailPoints)
                .flatMap((Collection::stream))
                .map(TimelineElement.FailPoint::getCategory)
                .forEach(c -> {
                    if (!categories.containsKey(c)) {
                        categories.put(c, new AtomicInteger(0));
                    }
                    categories.get(c).incrementAndGet();
                });
        final Element container = new Element("div");
        categories.keySet().stream().sorted().forEach(c -> {
            final Element label = new Element("label");
            final String id = c.getName() + "-" + jobName;
            label.attr("for", id);
            final Element checkbox = new Element("input");
            checkbox.attr("type", "checkbox")
                    .attr("name", id)
                    .attr("class", "category-filter")
                    .attr("id", id)
                    .attr("checked", "true");
            final Element point = new Element("div");
            point.addClass("legend-point")
                 .addClass(c.getShape().name().toLowerCase())
                 .attr("style", String.format("background-color: %s;", c.getColor()));
            final Element legend = new Element("span");
            legend.text(String.format("%s (%d)", c.getName().toLowerCase(), categories.get(c).get()));
            label.appendChild(checkbox).appendChild(point).appendChild(legend);
            container.appendChild(label);
        });
        return container;
    }

    private Element getTableFallsByTeams(final Timeline timeline, final String jobName) {
        final Set<String> header = new HashSet<>();
        header.add("");
        //получаю список с упавшими тестами
        List<TimelineElement> timelineInfraTests = timeline
                .getElements()
                .stream()
                .filter(e -> !e.isSkipped())
                .collect(Collectors.toList());
        final Set<IFailCategories> lineError = new HashSet<>();
        final List<List<String>> tableList = new ArrayList<>();
       // заполняю лан категории ошибок
        timelineInfraTests
                .stream()
                .filter(timelineElement -> timelineElement.getFailPoints().isEmpty()).forEach(e -> {
            header.add(e.getTeam());
            timelineInfraTests
                    .stream()
                    .map(TimelineElement::getFailPoints)
                    .flatMap((Collection::stream))
                    .map(TimelineElement.FailPoint::getCategory)
                    .forEach(lineError::add);
        });
        Element trHeader = new Element("tr");
        header.forEach(e -> {
            Element td = new Element("td").appendChild(new Element("strong").text(e));
            trHeader.appendChild(td);
        });
        final Element table = new Element("table");
        final Element tbody = new Element("tbody");
        table.addClass("infta_fail table " + jobName);
        tbody.appendChild(trHeader);
        tableList.add(new ArrayList<>(header));
        //формирую таблицу исходя из строк и заголовков
        lineError.forEach(e -> {
            Element tr = new Element("tr");
            Element tdLine = new Element("td");
            Element strongLine = new Element("strong");
            tdLine.appendChild(strongLine.text(e.getName()));
            tr.appendChild(tdLine);
            tbody.appendChild(tr);
            for (int a = 1; a < tableList.get(0).size(); a++) {
                Element td = new Element("td");
                Element strong = new Element("strong");
                int finalA = a;
                AtomicInteger atomicInteger = new AtomicInteger(0);
                timelineInfraTests
                        .forEach(element -> {
                            if (element.getTeam().equals(tableList.get(0).get(finalA)) && element
                                    .getFailPoints()
                                    .stream()
                                    .anyMatch(d -> d.getCategory().equals(e))) {
                                atomicInteger.incrementAndGet();

                            }
                        });
                //выделяю ячейку если имеются падения
                if (atomicInteger.get() != 0) {
                    td.attr("style", "background-color: #f08080");
                }
                strong.text(String.valueOf(atomicInteger.get()));
                td.appendChild(strong);
                tr.appendChild(td);
            }
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        return table;
    }
}
