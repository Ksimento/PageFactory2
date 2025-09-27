package ru.sbt.edu_power.external_services.confluence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class ConfluenceDocument {
    private final String pageID;
    private Document document;
    private int contentVersion;
    private String pageTitle;
    protected static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();

    protected ConfluenceDocument(final String pageID) {
        this.pageID = pageID;
    }

    protected void loadDocument() {
        final HttpResponse<JsonNode> response = ConfluenceConnect.getPage(
                pageID,
                ConfluenceConnect.Expand.BODY_STORAGE,
                ConfluenceConnect.Expand.VERSION
        );

        final String tableContent = response
                .getBody()
                .getObject()
                .getJSONObject("body")
                .getJSONObject("storage")
                .getString("value");
        document = Jsoup.parse(tableContent);
        document.select("col").remove();
        document.select("br").remove();
        contentVersion = response.getBody().getObject().getJSONObject("version").getInt("number");
        pageTitle = response.getBody().getObject().getString("title");
    }

    // очистка содержимого таблицы
    protected void removeTableData() {
        if (!getDocument().select("table *").isEmpty()) {
            getDocument().selectFirst("table").children().remove();
        }
    }

    // очистка или создание панели для вывода дополнительной информации в начале страницы
    protected void removePanelData() {
        if (!getDocument().select("pre > div").isEmpty()) {
            getDocument().selectFirst("pre > div").children().remove();
        } else {
            final Element section = new Element("pre");
            section.appendChild(new Element("div"));
            getDocument().selectFirst("body").insertChildren(0, section);
        }
    }

    protected void insertDataToPanel(final String text) {
        if (text.startsWith("<")) {
            getDocument().selectFirst("pre > div").append(text);
        } else {
            final Element p = new Element("p");
            p.text(text);
            getDocument().selectFirst("pre > div").appendChild(p);
        }

    }

    protected void insertHeaderToDocument(
            final List<TableHeaderInterface> tableCols,
            final Function<TableHeaderInterface, String> formatFunction
    ) {
        if (getDocument().select("table").isEmpty()) {
            getDocument().selectFirst("body").appendChild(new Element("table"));
        }
        final Element row = getDocument()
                .selectFirst("table")
                .appendChild(new Element("thead"))
                .child(0)
                .appendChild(new Element("tr"))
                .child(0);
        for (final TableHeaderInterface tableHeader : tableCols) {
            if (tableHeader.doNotRender()) {
                continue;
            }
            final Element th = new Element("th");
            th.text(formatFunction.apply(tableHeader));
            row.appendChild(th);
        }
    }

    protected void saveDocument() {
        final String resultHtml = getDocument().body().html().replaceAll("\\n\\s*", "");
        contentVersion++;
        final ConfluenceContentModel model = new ConfluenceContentModel(contentVersion, pageTitle, resultHtml);
        ConfluenceConnect.updatePage(pageID, model);
    }

    protected String formatHeader(final String header, final int spaceChars) {
        final char spaceChar = 160;
        return IntStream
                .range(0, spaceChars)
                .mapToObj(i -> String.valueOf(spaceChar))
                .collect(Collectors.joining("", header.replace(" ", String.valueOf(spaceChar)), ""));
    }

    protected void insertDataToTable(
            final List<String> rowNames,
            final List<TableHeaderInterface> headers,
            final BiFunction<String, TableHeaderInterface, String> converterFunction
    ) {
        getDocument().selectFirst("table").appendChild(new Element("tbody"));
        for (final String rowName : rowNames) {
            final Element row = getDocument().selectFirst("tbody").appendChild(new Element("tr")).children().last();
            for (final TableHeaderInterface header : headers) {
                if (header.doNotRender()) {
                    continue;
                }
                final Element td = new Element("td");
                final String text = converterFunction.apply(rowName, header);
                if (text.contains("\n")) {
                    Stream.of(text.split("\n"))
                          .forEach(e -> {
                              final Element p = new Element("p");
                              p.text(e);
                              td.appendChild(p);
                          });
                } else {
                    if (text.startsWith("<")) {
                        td.append(text);
                    } else {
                        td.text(text);
                    }
                }
                row.appendChild(td);
            }
        }
    }

    protected void insertTotalRowToTable(
            final List<TableHeaderInterface> headers,
            final Function<TableHeaderInterface, String> converterFunction,
            final TableHeaderInterface totalHeaderName
    ) {
        final Element row = getDocument().selectFirst("tbody").appendChild(new Element("tr")).children().last();
        row.appendChild(new Element("td")).children().last().text("Итого");
        for (final TableHeaderInterface header : headers) {
            if (header.equals(totalHeaderName) || header.doNotRender()) {
                continue;
            }
            final Element b = new Element("b");
            final Element td = new Element("td");
            final String total = converterFunction.apply(header);
            b.text(total);
            td.appendChild(b);
            row.appendChild(td);
        }
    }

    protected Document getDocument() {
        return document;
    }

    public static String getUserDisplayName(final String name) {
        final JiraUser user = JiraConnect.jiraGetUser(name);
        return user.getDisplayName();
    }

    public static String getUserName(final String name) {
        final JiraUser user = JiraConnect.jiraGetUser(name);
        return user.getName();
    }
}
