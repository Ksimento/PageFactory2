package ru.sbt.edu_power.e2e_core.api.connect;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.stream.Collectors;

public class HtmlParser {

    public List<String> parseTagAttribute(final String html, final String tagName, final String attributeName) {
        final Document document = Jsoup.parse(html);
        final Elements elements = document.getElementsByTag(tagName);

        return elements.stream().map(e -> e.attributes().get(attributeName)).collect(Collectors.toList());
    }
}
