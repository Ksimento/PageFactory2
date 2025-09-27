package ru.sbt.edu_power.assist_bot.tasks.system.user_roles_config;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.CheckboxesElement;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractCheckBoxFormField;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackUserSelectSection;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public class UserRolesCheckBoxSection extends AbstractSection {

    public UserRolesCheckBoxSection(
            final SlackUserSelectSection slackUserSelectSection
    ) {
        super(new Accessory(slackUserSelectSection));
    }

    public UserRolesCheckBoxSection(
            final SlackUserSelectSection slackUserSelectSection,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(slackUserSelectSection), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText("Выбери роли для пользователя", false))
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractCheckBoxFormField {
        private final SlackUserSelectSection slackUserSelectSection;

        Accessory(final SlackUserSelectSection slackUserSelectSection) {
            this.slackUserSelectSection = slackUserSelectSection;
        }

        @Override
        public BlockElement getElement() {
            clearState();
            final List<OptionObject> options = getOptions();
            final List<OptionObject> initial = options
                    .stream()
                    .filter(option -> Main.USER_ROLES_REPOSITORY.hasAny(
                            slackUserSelectSection.getAccessory().getValue(),
                            SlackRoles.valueOf(option.getValue())))
                    .collect(Collectors.toList());
            return CheckboxesElement.builder()
                                    .actionId(getId())
                                    .options(options)
                                    .initialOptions(initial)
                                    .build();
        }

        @Override
        public String getName() {
            return null;
        }

        private List<OptionObject> getOptions() {
            return Arrays.stream(SlackRoles.values())
                         .map(role -> OptionObject.builder()
                                                  .value(role.name())
                                                  .text(asText(role.name(), false))
                                                  .build()
                         )
                         .collect(Collectors.toList());
        }
    }
}
