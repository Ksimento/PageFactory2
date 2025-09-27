package ru.sbt.edu_power.external_services.bitbucket;

public class BBConnectionException extends RuntimeException {
    private static final long serialVersionUID = -5547863707497555955L;

    public BBConnectionException(final String message) {
        super(message);
    }
}
