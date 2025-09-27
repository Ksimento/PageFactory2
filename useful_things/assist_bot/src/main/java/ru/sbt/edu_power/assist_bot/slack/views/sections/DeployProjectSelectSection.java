package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

/**
 * Секция для модалки для выбора какой фронт приложения деплоить - ШЦП или Ш21
 */
public class DeployProjectSelectSection extends AbstractSection {

    public DeployProjectSelectSection() {
        super(new Accessory());
    }

    public DeployProjectSelectSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                .blockId(getId())
                .text(asText("Проект для деплоя", false))
                .accessory(getAccessory().getElement())
                .build();
    }

    private static class Accessory extends AbstractSelectFormField {
        private final String name = "Проект для деплоя";

        @Override
        public BlockElement getElement() {
            return getSelect(
                    name,
                    Arrays.asList(
                            OptionObject.builder().text(asText("edupower", false)).value("edupower").build(),
                            OptionObject.builder().text(asText("school21", false)).value("school21").build()
                    )
            );
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
