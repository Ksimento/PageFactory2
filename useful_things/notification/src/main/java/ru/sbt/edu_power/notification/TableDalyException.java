package ru.sbt.edu_power.notification;

public class TableDalyException extends RuntimeException {
    private static final long serialVersionUID = -3878238451820516415L;

    public TableDalyException(final String message) {
        super(message);
    }

    public TableDalyException(final Throwable e) {
        super(e);
    }
}
