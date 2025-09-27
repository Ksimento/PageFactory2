package ru.sbt.edu_power.smoke_report;

public class SmokeReportError extends RuntimeException{
    private static final long serialVersionUID = -2246592967738513430L;

    public SmokeReportError(final String message) {
        super(message);
    }
}
