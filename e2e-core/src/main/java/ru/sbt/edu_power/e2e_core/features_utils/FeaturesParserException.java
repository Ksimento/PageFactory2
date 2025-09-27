package ru.sbt.edu_power.e2e_core.features_utils;

public class FeaturesParserException extends RuntimeException {
    private static final long serialVersionUID = -1037685647089077804L;

    public FeaturesParserException(final String message) {
        super(message);
    }

    public FeaturesParserException(final Throwable e) {
        super(e);
    }
}
