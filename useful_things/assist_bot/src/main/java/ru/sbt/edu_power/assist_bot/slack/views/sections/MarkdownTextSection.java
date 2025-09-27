package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Секция для модалки с возможностью вывести произвольный текст
 */
public class MarkdownTextSection extends AbstractSection {
    private final Supplier<String> mrkdnText;

    public MarkdownTextSection(final Supplier<String> mrkdnText) {
        super(null);
        this.mrkdnText = mrkdnText;
    }

    public MarkdownTextSection(
            final Supplier<String> mrkdnText,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(null, isAndCondition, constructConditions);
        this.mrkdnText = mrkdnText;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asMrkdn(mrkdnText.get()))
                           .blockId(getId())
                           .build();
    }
}
