package ru.sbt.edu_power.assist_bot;

public class AssistBotException extends RuntimeException {
    private static final long serialVersionUID = 723588790588456985L;

    public AssistBotException(final String message) {
        super(message);
    }

    public AssistBotException(final Throwable e) {
        super(e);
    }

    public AssistBotException(final String message, final Throwable e) {
        super(message, e);
    }
}
