package ru.sbt.edu_power.assist_bot.slack.views;

import com.slack.api.bolt.App;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.views.sections.FieldUtils;

public class Element {
    public static final App APP = Main.SLACK_DISPATCHER.getApp();
    public static final String BOT_TOKEN = Main.BOT_TOKEN;
    protected PlainTextObject asText(final String text, final boolean emoji) {
        return PlainTextObject.builder()
                              .text(text)
                              .emoji(emoji)
                              .build();
    }

    protected PlainTextObject asMiddleCutText(final String text, final int maxLength, final boolean emoji) {
        return asText(FieldUtils.optionCut(text, maxLength, FieldUtils.CutType.MIDDLE), emoji);
    }

    protected MarkdownTextObject asMrkdn(final String text) {
        return MarkdownTextObject.builder()
                .text(text)
                .build();
    }

    protected ViewTitle asViewTitle(final String text, final boolean emoji) {
        return ViewTitle.builder()
                        .text(text)
                        .type("plain_text")
                        .emoji(emoji)
                        .build();
    }

    protected ViewClose asViewClose(final String text, final boolean emoji) {
        return ViewClose.builder()
                        .type("plain_text")
                        .text(text)
                        .emoji(emoji)
                        .build();
    }

    protected ViewSubmit asViewSubmit(final String text, final boolean emoji) {
        return ViewSubmit.builder()
                         .type("plain_text")
                         .text(text)
                         .emoji(emoji)
                         .build();
    }

    protected OptionObject asOptionObject(final String text, final String value) {
        return OptionObject.builder()
                           .text(asText(text, false))
                           .value(value)
                           .build();
    }

    protected OptionObject asOptionObject(final String text, final String value, final String description) {
        return OptionObject.builder()
                           .text(asText(text, false))
                           .value(value)
                           .description(asText(description, true))
                           .build();
    }
}
