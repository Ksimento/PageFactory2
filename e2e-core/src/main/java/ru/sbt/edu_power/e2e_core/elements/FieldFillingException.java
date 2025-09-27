package ru.sbt.edu_power.e2e_core.elements;

public class FieldFillingException extends Exception {
    private static final long serialVersionUID = 8272069831917244373L;

    public FieldFillingException(final String message) {
        super(message);
    }

    public FieldFillingException(final Throwable cause) {
        super(cause);
    }

    public FieldFillingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
