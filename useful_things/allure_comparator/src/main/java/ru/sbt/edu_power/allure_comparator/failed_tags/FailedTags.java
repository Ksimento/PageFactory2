package ru.sbt.edu_power.allure_comparator.failed_tags;

import org.jsoup.nodes.Element;
import ru.sbt.edu_power.allure_comparator.report.Report;

import java.util.Objects;

public class FailedTags extends Report {


    public FailedTags(final String name) {
        super(name);
    }

    public void generate(final String projectKey) {
        document.body().appendChild(getBlock(projectKey));
    }

    public Element getBlock(final String projectKey) {
        final Element ul = new Element("ul");
        sectionList.forEach(section -> {
            final Element e = section.failedTagsSection(projectKey);
            if (Objects.isNull(e)) {
                return;
            }
            ul.appendChild(e);
        });
        return ul;
    }
}