package ru.sbt.edu_power.confluence_reporting;


import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Optional;

/**
 * Класс реализует возможность раскрашивать таблицу
 */
public class TableTune {
    private final Document document;

    public TableTune(final Document document) {
        this.document = document;
    }

    public Optional<Element> getTableRowByText(final String team) {
        final Elements elements = document.select("tbody tr");
        return elements.stream().filter(row -> row.select("td").eachText().contains(team)).findFirst();
    }

    public Optional<Element> getTableCellByText(final String team) {
        return document.select("tbody td").stream().filter(td -> team.equals(td.text())).findFirst();
    }

    public void markRow(final Element row, final BgColor bgColor) {
        row.select("td").forEach(cell -> addBgColor(cell, bgColor));
    }

    public void markCell(final Element cell, final BgColor bgColor) {
        addBgColor(cell, bgColor);
    }

    private void addBgColor(final Element element, final BgColor bgColor) {
        element.attr("class", bgColor.classAttr);
        element.attr("data-highlight-colour", bgColor.dataHighlightColour);
    }

    public void addTitle(final Element element, final String titleText) {
        element.attr("title", titleText);
    }

    public enum BgColor {
        RED("highlight-red", "red"),
        GREY("highlight-grey", "grey"),
        GREEN("highlight-green", "green"),
        BLUE("highlight-blue", "blue"),
        YELLOW("highlight-yellow", "yellow");

        private final String classAttr;
        private final String dataHighlightColour;

        BgColor(final String classAttr, final String dataHighlightColour) {
            this.classAttr = classAttr;
            this.dataHighlightColour = dataHighlightColour;
        }

        public String getClassAttr() {
            return classAttr;
        }

        public String getDataHighlightColour() {
            return dataHighlightColour;
        }
    }
}
