package ru.sbt.edu_power.external_services;

public class ExternalServicesException extends RuntimeException {
    private static final long serialVersionUID = 7056718365354514248L;

    public ExternalServicesException(final Throwable e) {
        super(e);
    }

    public ExternalServicesException(final String message) {
        super(message);
    }

    public ExternalServicesException(final String message, final Throwable e) {
        super(message, e);
    }
}
