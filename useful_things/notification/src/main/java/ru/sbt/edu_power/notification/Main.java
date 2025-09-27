package ru.sbt.edu_power.notification;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;

import org.jsoup.Jsoup;
import ru.sbt.edu_power.external_services.confluence.ConfluenceConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnect;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(final String[] args) throws ParseException {

        if(DalyProjectValue.isSkipDay(new Date())) {
            JiraConnect.configureConnection();
            List<DalyProjectValue> dalyProjectsValues = DalyProjectValue.getDalyProjectValue();
            for(DalyProjectValue dalyProjectValues : dalyProjectsValues) {
                HttpResponse<JsonNode> pageDaly = ConfluenceConnect.getPage(
                        dalyProjectValues.getDalyTable(),
                        ConfluenceConnect.Expand.BODY_VIEW
                );
                final String tableContent = pageDaly
                        .getBody()
                        .getObject()
                        .getJSONObject("body")
                        .getJSONObject("view")
                        .getString("value");
                new Table(dalyProjectValues).mmNotificationDaly(Jsoup.parse(tableContent));
            }
        }
    }
}
