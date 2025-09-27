package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.RegressTestRunCreation;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Секция для модалки для выбора пресета для создания тест-сета
 */
public class PresetSelectSection extends AbstractSection {

    public PresetSelectSection() {
        super(new Accessory());
    }

    public PresetSelectSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText("Пресет для создания тест-сета", false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    public static class Accessory extends AbstractSelectFormField {
        private final String name = "Пресет для создания тест-сета";

        @Override
        public BlockElement getElement() {
            return getSelect(
                    name,
                    Stream.of(RegressTestRunCreation.Preset.values())
                          .map(p -> asOptionObject(
                                  p.name(),
                                  p.name(),
                                  p.getDescription()
                          ))
                          .collect(Collectors.toList())
            );
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
