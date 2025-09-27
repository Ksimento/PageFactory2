package ru.sbt.edu_power.assist_bot.slack;

public class SlackError extends Error {

    private static final long serialVersionUID = -1623357909706432695L;

    public SlackError(final String message) {
        super(message);
    }

    public SlackError(final Throwable e) {
        super(e);
    }
}
