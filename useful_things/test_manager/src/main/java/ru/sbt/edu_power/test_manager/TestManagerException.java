package ru.sbt.edu_power.test_manager;

public class TestManagerException extends RuntimeException {
    private static final long serialVersionUID = -3364708722619998474L;

    public TestManagerException(final String message) {
        super(message);
    }

    public TestManagerException(final Throwable e) {
        super(e);
    }

    public TestManagerException(final String message, final Throwable e) {
        super(message, e);
    }
}
