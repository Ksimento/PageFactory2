package ru.sbt.edu_power.allure_job_general_report;

public class AllureJobGeneralReportException extends RuntimeException {
    private static final long serialVersionUID = 873733569743194046L;

    public AllureJobGeneralReportException(final String message) {
        super(message);
    }

    public AllureJobGeneralReportException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public AllureJobGeneralReportException(final Throwable cause) {
        super(cause);
    }
}
