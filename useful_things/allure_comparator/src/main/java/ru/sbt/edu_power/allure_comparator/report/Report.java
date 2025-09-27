package ru.sbt.edu_power.allure_comparator.report;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Report {
    protected final List<ReportSection> sectionList = new ArrayList<>();
    protected final Document document;

    @SneakyThrows
    public Report(final String name) {
        final String baseUrl = Objects
                .requireNonNull(this.getClass().getClassLoader().getResource(name)).toString();
        document = Jsoup.parse(
                this.getClass().getClassLoader().getResourceAsStream(name),
                StandardCharsets.UTF_8.name(),
                baseUrl
        );
    }

    public void addHtmlMessage(final String message) {
        final Element element = new Element("div");
        element.attr("class", "message");
        final Element inner = new Element("div");
        element.appendChild(inner);
        inner.append(message);
        document.body().appendChild(element);
    }

    public void addSection(final ReportSection section) {
        sectionList.add(section);
    }

    public String getContent() {
        return document.toString();
    }

    public void generate() {
        document.body().appendChild(getBlock());
    }

    protected Element getBlock() {
        final Element ul = new Element("ul");
        sectionList.forEach(section -> {
            final Element e = section.getHiddenSection();
            if (Objects.isNull(e)) {
                return;
            }
            ul.appendChild(e);
        });
        return ul;
    }
}
