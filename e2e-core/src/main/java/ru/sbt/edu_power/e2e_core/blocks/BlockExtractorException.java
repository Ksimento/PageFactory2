package ru.sbt.edu_power.e2e_core.blocks;

public class BlockExtractorException extends RuntimeException {
    private static final long serialVersionUID = -3647316287878901524L;

    public BlockExtractorException(final String message) {
        super(message);
    }

    public BlockExtractorException(final String message, final Throwable e) {
        super(message, e);
    }
}
