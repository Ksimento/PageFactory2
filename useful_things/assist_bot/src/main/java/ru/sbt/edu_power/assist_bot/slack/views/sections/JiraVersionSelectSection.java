package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

// возвращает ID версии (циферное обозначение)
public class JiraVersionSelectSection extends AbstractSection {
    private final JiraProjectSelectSection jiraProjectSelectSection;
    private final String name;

    public JiraVersionSelectSection(final JiraProjectSelectSection jiraProjectSelectSection) {
        super(new Accessory(jiraProjectSelectSection));
        this.jiraProjectSelectSection = jiraProjectSelectSection;
        name = "Версия приложения";
    }

    public JiraVersionSelectSection(final JiraProjectSelectSection jiraProjectSelectSection, final String name) {
        super(new Accessory(jiraProjectSelectSection));
        this.jiraProjectSelectSection = jiraProjectSelectSection;
        this.name = name;
    }

    public JiraVersionSelectSection(
            final JiraProjectSelectSection jiraProjectSelectSection,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(jiraProjectSelectSection), isAndCondition, constructConditions);
        this.jiraProjectSelectSection = jiraProjectSelectSection;
        name = "Версия приложения";
    }

    public JiraVersionSelectSection(
            final JiraProjectSelectSection jiraProjectSelectSection,
            final String name,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(jiraProjectSelectSection), isAndCondition, constructConditions);
        this.jiraProjectSelectSection = jiraProjectSelectSection;
        this.name = name;
    }

    @Override
    public boolean constructCondition() {
        return jiraProjectSelectSection.getAccessory().isFilled() && super.constructCondition();
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText(name, false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractSelectFormField {
        private final JiraProjectSelectSection jiraProjectSelectSection;
        private final String name = "Выбрать версию";

        Accessory(final JiraProjectSelectSection jiraProjectSelectSection) {
            this.jiraProjectSelectSection = jiraProjectSelectSection;
        }

        @Override
        public BlockElement getElement() {
            return getCachedElement(() -> getSelect(name, getOptions()), jiraProjectSelectSection.getAccessory().getValue());
        }

        private List<OptionObject> getOptions() {
            final TCFields.ProjectId project = TCFields.ProjectId.valueOf(jiraProjectSelectSection.getAccessory().getValue());
            return new JiraVersion()
                    .getNotReleasedVersions(project.id)
                    .stream()
                    .map(v -> asOptionObject(project.name() + ": " + v.getName(), v.getId()))
                    .collect(Collectors.toList());
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
