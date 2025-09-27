package ru.sbt.edu_power.confluence_reporting;

import ru.sbt.edu_power.confluence_reporting.RTM_metrics.GenerateRtmAuditTableNF;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.GenerateRtmAuditTableRegress;
import ru.sbt.edu_power.external_services.jira.JiraConnect;

// Тестовый запуск кода можно выполнить в классе test/java/ReportTest.java
public class ReportRunner {
    public static void main(final String[] args) {
        JiraConnect.configureConnection();
        final GenerateRtmAuditTableNF table = new GenerateRtmAuditTableNF("52693413", "EDU");
        table.generate();
    }
}
