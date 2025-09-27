package ru.sbt.edu_power.allure_comparator.report;

import org.jsoup.nodes.Element;
import ru.sbt.edu_power.allure_comparator.ChildrenPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportSection {
    private final String name;
    private final String sectionClass;
    private final List<ChildrenPair> childrenList = new ArrayList<>();
    private final List<ReportSection> sectionList = new ArrayList<>();

    public ReportSection(final String name) {
        this.name = name;
        this.sectionClass = "default";
    }

    public ReportSection(final String name, final String sectionClass) {
        this.name = name;
        this.sectionClass = sectionClass;
    }

    public void addChildren(final ChildrenPair children) {
        childrenList.add(children);
    }

    public void addSection(final ReportSection section) {
        sectionList.add(section);
    }

    private Element getLine(final ChildrenPair pair) {
        final Element currentAnchor = new Element("a");
        currentAnchor.attr("href", pair.fst.getLink())
                     .text(pair.fst.getName());

        final Element li = new Element("li");
        li.appendChild(currentAnchor);

        // Если текущий и прошлый тест разные - добавляем прошлый тест в отчёт
        if (!pair.fst.equals(pair.snd)) {
            final Element pastAnchor = new Element("a");
            pastAnchor.attr("href", pair.snd.getLink())
                      .attr("class", "secondary")
                      .text("[предыдущий тест]");

            final Element spaceDivider = new Element("span");
            spaceDivider.text(" ");

            li.appendChild(spaceDivider);
            li.appendChild(pastAnchor);
        }
        li.appendChild(new Element("br"));
        li.attr("class", pair.fst.getStatus() + " numeric");

        if (Objects.nonNull(pair.fst.getReport().getStatusMessage())) {
            final Element message = new Element("span");
            final String messageText = pair.fst.getReport().getStatusMessage().replaceAll("\n", " ");
            final String trimmedText = messageText.substring(0, Math.min(messageText.length(), 200));
            message.text(trimmedText);
            li.appendChild(message);
        }
        return li;
    }

    private Element getHidedBlock() {
        final Element ul = new Element("ul");
        ul.attr("class", "hided-block");
        return ul;
    }

    public Element getHiddenSection() {
        final int elementsCount = getElementsCount();
        if (childrenList.isEmpty() && sectionList.isEmpty()) {
            return null;
        }
        final String id = UUID.randomUUID().toString();

        final Element label = new Element("label");
        label.attr("for", id)
             .text(name);

        if (elementsCount > 0) {
            label.attr("class", "has-data");
        }

        final Element counter = new Element("span");
        counter.text("(" + elementsCount + ")");

        label.appendChild(counter);

        final Element checkbox = new Element("input");
        checkbox.attr("type", "checkbox")
                .attr("id", id)
                .attr("class", "hidden")
                .attr("checked", true);

        final Element hidedBlock = getHidedBlock();
        sectionList.forEach(b -> {
            final Element e = b.getHiddenSection();
            if (Objects.isNull(e)) {
                return;
            }
            hidedBlock.appendChild(e);
        });
        childrenList.forEach(c -> hidedBlock.appendChild(getLine(c)));

        final Element li = new Element("li");
        li.attr("class", sectionClass);
        li.appendChild(checkbox);
        li.appendChild(label);
        li.appendChild(hidedBlock);

        return li;
    }

    public Element failedTagsSection(String projectKey) {
        final Element li = new Element("li");
        if (childrenList.isEmpty() && sectionList.isEmpty()) {
            return null;
        }
        sectionList.forEach(b -> {
                    if (b.childrenList.isEmpty()) {
                        return;
                    }
                    Element name = new Element("label");
                    name.text(b.name);
                    Element teamTags = getHidedBlock();
                    final Element tag = new Element("li");
                    List<String> listTag = new ArrayList<>();
                    switch (projectKey) {
                        case "S21":
                            b.childrenList.forEach(c -> listTag.add("@" + c.fst.searchTags("S21-T").get(0)));
                            break;
                        case "EDU":
                            b.childrenList.forEach(c -> listTag.add("@" + c.fst.searchTags("EDU-T").get(0)));
                            break;
                        case "BTC":
                            b.childrenList.forEach(c -> listTag.add("@" + c.fst.searchTags("BTC-T").get(0)));
                    }

                    li.attr("class", sectionClass);
                    tag.text(String.join(" or ", listTag));
                    teamTags.appendChild(tag);
                    li.appendChild(name);
                    li.appendChild(teamTags);
                }
        );
        return li;
    }

    int getElementsCount() {
        final AtomicInteger count = new AtomicInteger(childrenList.size());
        sectionList.forEach(s -> count.addAndGet(s.getElementsCount()));
        return count.get();
    }
}
