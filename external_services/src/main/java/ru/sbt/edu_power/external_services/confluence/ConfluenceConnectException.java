package ru.sbt.edu_power.external_services.confluence;

public class ConfluenceConnectException extends RuntimeException {
    private static final long serialVersionUID = -3878238451820516415L;

    public ConfluenceConnectException(final String message) {
        super(message);
    }

    public ConfluenceConnectException(final Throwable e) {
        super(e);
    }
}
