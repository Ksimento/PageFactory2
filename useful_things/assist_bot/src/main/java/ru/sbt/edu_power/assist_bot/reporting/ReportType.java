package ru.sbt.edu_power.assist_bot.reporting;

public enum ReportType {
    REGRESS("Ассистент регресса"),
    IFT("IFT ассистент"),
    TOTAL_REPORT_TO_RELEASE("Статус релиза");

    private final String reportName;

    ReportType(final String reportName) {
        this.reportName = reportName;
    }

    public String getReportName() {
        return reportName;
    }
}
