package ru.sbt.edu_power.allure_comparator;

public class AllureComparatorException extends RuntimeException {
    private static final long serialVersionUID = -7947764913320902062L;

    public AllureComparatorException(final String message) {
        super(message);
    }

    public AllureComparatorException(final Throwable t) {
        super(t);
    }

    public AllureComparatorException(final String message, final Throwable t) {
        super(message, t);
    }
}
