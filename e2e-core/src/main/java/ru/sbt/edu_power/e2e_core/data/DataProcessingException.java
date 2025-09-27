package ru.sbt.edu_power.e2e_core.data;

public class DataProcessingException extends RuntimeException {
    private static final long serialVersionUID = 2330750568820077443L;

    public DataProcessingException(final Throwable e) {
        super(e);
    }

    public DataProcessingException(final String message) {
        super(message);
    }
}
