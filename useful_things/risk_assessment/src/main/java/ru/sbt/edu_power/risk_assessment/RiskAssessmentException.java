package ru.sbt.edu_power.risk_assessment;

public class RiskAssessmentException extends RuntimeException {
    private static final long serialVersionUID = 8163379524873982203L;

    public RiskAssessmentException(final String message) {
        super(message);
    }

    public RiskAssessmentException(final Throwable e) {
        super(e);
    }
}
