package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractUserSelectFormField;

import java.util.function.BooleanSupplier;

public class UserWithDifferentEmailSelectSection extends AbstractSection {
    private final JiraUser jiraUser;

    public UserWithDifferentEmailSelectSection(final JiraUser jiraUser) {
        super(new Accessory());
        this.jiraUser = jiraUser;
    }

    public UserWithDifferentEmailSelectSection(
            final JiraUser jiraUser,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
        this.jiraUser = jiraUser;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText(jiraUser.getDisplayName(), false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    public JiraUser getJiraUser() {
        return jiraUser;
    }

    private static class Accessory extends AbstractUserSelectFormField {
        private final String name = "Выбрать пользователя";

        @Override
        public BlockElement getElement() {
            return getUserSelect(name);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
