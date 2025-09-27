package ru.sbt.edu_power.e2e_core.confluence_integration;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.e2e_core.page_tags.PageChildren;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeneratePlatformGraph extends ConfluenceDocument {
    private final List<PageChildren> pageChildrenList;
    private final Map<String, Integer> tagCounter;

    public GeneratePlatformGraph(
            final String pageID,
            final List<PageChildren> pageChildrenList,
            final Map<String, Integer> tagCounter
    ) {
        super(pageID);
        this.pageChildrenList = pageChildrenList;
        this.tagCounter = tagCounter;
    }

    public void generate() {
        loadDocument();
        prepareDocument();
        final Elements eLements = getDocument().select(new Evaluator.TagEndsWith("layout-cell"));
        for (final Element element : eLements) {
            if (element.children().isEmpty()) {
                pageChildrenList.forEach(pageChildren ->
                        element
                                .appendChild(
                                        getExpandedItem(pageChildren)
                                )
                );
            }
        }
        saveDocument();
    }

    private void prepareDocument() {
        final Elements elements = getDocument().select(new Evaluator.TagEndsWith("layout-cell"));
        for (final Element element : elements) {
            element.select(new Evaluator.AttributeWithValue("ac:name", "expand")).remove();
        }
        getDocument().select("br").remove();
    }

    private Element getExpandedItem(final PageChildren pageChildren) {
        final String title = pageChildren.getName();
        final String tag = pageChildren.getTag();
        final List<Element> expandedElementList = new ArrayList<>();
        pageChildren.getChildrenList().forEach(children -> expandedElementList.add(getExpandedItem(children)));
        final Element titleParameter = new Element("ac:parameter")
                .attr("ac:name", "title")
                .text(title);
        final Element richTextBody = new Element("ac:rich-text-body");
        final Element expandMacro = new Element("ac:structured-macro")
                .attr("ac:name", "expand")
                .attr("ac:schema-version", "1")
                .attr("ac:macro-id", UUID.randomUUID().toString());
        if (!tag.isEmpty()) {
            if (tagCounter.containsKey(tag)) {
                final Element tagElement = new Element("p").text(tag);
                richTextBody.appendChild(tagElement);
                final String tagCount = "Тестов: " + tagCounter.get(tag);
                final Element tagCountElement = new Element("p").text(tagCount);
                richTextBody.appendChild(tagCountElement);
            }
        }
        if (!expandedElementList.isEmpty()) {
            final Element innerPage = new Element("p").text("Вложенные страницы: ");
            richTextBody.appendChild(innerPage);
        }
        expandedElementList.forEach(richTextBody::appendChild);
        expandMacro
                .appendChild(titleParameter)
                .appendChild(richTextBody);
        return expandMacro;
    }
}
