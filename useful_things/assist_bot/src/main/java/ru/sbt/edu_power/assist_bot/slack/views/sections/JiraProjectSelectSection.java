package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Секция для модалки для выбора проектной области джиры
 * Сохраняет имя проектной области (например S21 или EDU)
 */
public class JiraProjectSelectSection extends AbstractSection {

    public JiraProjectSelectSection() {
        super(new Accessory());
    }

    public JiraProjectSelectSection(final boolean isAndCondition, final BooleanSupplier... constructConditions) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText("Проектная область", false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractSelectFormField {
        private final String name = "Выбрать проект";

        @Override
        public BlockElement getElement() {
            return getSelect(
                    name,
                    Stream.of(TCFields.ProjectId.values())
                          .map(p -> asOptionObject(p.name(), p.name()))
                          .collect(Collectors.toList())
            );
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
