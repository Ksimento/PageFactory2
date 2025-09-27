package ru.sbt.edu_power.notification;

import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum DalyProjectValue {
    EDU(
            "37519989",
            "50958954",
            "samolin.d.se"
    ),
    S21(
            "37519989",
            "76685805",
            "pakartakov"
    );
    final String dalyInfo;
    final String dalyTable;
    final String automationLead;
    final String smokeUrlStand;
    final String smokeJobUrl;
    final String textNotification;

    DalyProjectValue(final String dalyInfo, final String dalyTable, final String automationLead) {
        this.dalyInfo = dalyInfo;
        this.dalyTable = dalyTable;
        this.smokeUrlStand = System.getProperty("smokeUrlStand");
        this.smokeJobUrl = System.getProperty("smokeJobUrl");
        this.automationLead = automationLead;
        this.textNotification  = NotificationText.getNotificationText(this);
    }

    public static String getProjectNotification() {
        return System.getProperty("jiraProjectKey");
    }

    public static String getHttpViewpage() {
        return "https://confluence.pcbltools.ru/confluence/pages/viewpage.action?pageId=";
    }

    public static boolean isSkipDay(final Date date) {
        final String day = new SimpleDateFormat("EEEE").format(date).toLowerCase();
        return !(day.equals("суббота") | day.equals("воскресенье"));
    }

    public static List<DalyProjectValue> getDalyProjectValue() {
        final String project = getProjectNotification();
        if (project.equals("ALL")) {
            return Arrays.stream(DalyProjectValue.values()).collect(Collectors.toList());
        } else if (project.equals("MFE")) {
            return Collections.singletonList(DalyProjectValue.valueOf("EDU"));
        } else {
            return Collections.singletonList(DalyProjectValue.valueOf(getProjectNotification()));
        }
    }
}
