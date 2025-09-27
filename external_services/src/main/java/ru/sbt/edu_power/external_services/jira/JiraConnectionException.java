package ru.sbt.edu_power.external_services.jira;

public class JiraConnectionException extends RuntimeException {
    private static final long serialVersionUID = -5009313884779437528L;

    public JiraConnectionException(final Throwable e) {
        super(e);
    }

    public JiraConnectionException(final String message, final Throwable e) {
        super(message, e);
    }

    public JiraConnectionException(final String message) {
        super(message);
    }
}
