package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSingleCheckBoxFormField;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.function.BooleanSupplier;

/**
 * Секция для модалки с чекбоксом выбора стенда для каталога
 */
public class EnvCatalogCheckBoxSection extends AbstractSection {
    public EnvCatalogCheckBoxSection() {
        super(new Accessory());
    }

    public EnvCatalogCheckBoxSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                .blockId(getId())
                .accessory(getAccessory().getElement())
                .text(asText("Так же я задеплою каталог (последний develop) на стенд, либо нужно поставить галочку", true))
                .build();
    }

    private static class Accessory extends AbstractSingleCheckBoxFormField {
        private final String name = "Нет, использовать общий стенд для каталога";
        private static final String DESCRIPTION = "Если чекбокс снят, будет использоваться текущий стенд";

        @Override
        public BlockElement getElement() {
            return getCheckBox(name, DESCRIPTION);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
