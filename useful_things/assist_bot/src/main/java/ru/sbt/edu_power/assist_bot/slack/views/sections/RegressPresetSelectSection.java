package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.RegressPreset;

import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Секция для модалки с возможностью выбора пресета для запуска регресса
 */
public class RegressPresetSelectSection extends AbstractSection {
    public RegressPresetSelectSection() {
        super(new Accessory());
    }

    public RegressPresetSelectSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        final String description = Stream.of(RegressPreset.values())
                .map(p -> "*" + p.name() + "*: " + p.getDescription())
                .collect(Collectors.joining("\n"));
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asMrkdn("Пресет для прогона AFT\n" + description))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    public static class Accessory extends AbstractSelectFormField {
        private final String name = "Пресет для создания тест-сета";

        @Override
        public BlockElement getElement() {
            return getSelect(
                    name,
                    Stream.of(RegressPreset.values())
                          .map(p -> asOptionObject(
                                  p.name(),
                                  p.name()
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
