package ru.sbt.edu_power.external_services.validator;

public class ValidateException extends RuntimeException {
    private static final long serialVersionUID = -1157499406364336065L;

    public ValidateException(final String message) {
        super(message);
    }

    public ValidateException(final Throwable e) {
        super(e);
    }
}
