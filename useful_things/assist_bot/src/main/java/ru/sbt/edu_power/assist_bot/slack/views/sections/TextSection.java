package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Секция для модалки с возможностью вывести произвольный текст
 */
public class TextSection extends AbstractSection {
    private final Supplier<String> text;

    public TextSection(final Supplier<String> text) {
        super(null);
        this.text = text;
    }

    public TextSection(
            final Supplier<String> text,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(null, isAndCondition, constructConditions);
        this.text = text;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText(text.get(), true))
                           .blockId(getId())
                           .build();
    }
}
