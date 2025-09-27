package ru.sbt.edu_power.external_services.nexus;

public class NexusConnectException extends RuntimeException {
    private static final long serialVersionUID = -383524144031543223L;

    public NexusConnectException(final String message) {
        super(message);
    }
}
