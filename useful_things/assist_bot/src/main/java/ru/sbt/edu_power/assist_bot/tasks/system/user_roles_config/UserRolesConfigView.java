package ru.sbt.edu_power.assist_bot.tasks.system.user_roles_config;

import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.MarkdownTextSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackUserSelectSection;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserRolesConfigView extends AbstractModal {
    private final SlackUserSelectSection slackUserSelectSection = new SlackUserSelectSection(
            "Выбери пользователя для которого нужно установить роли"
    );

    private final UserRolesCheckBoxSection userRolesCheckBoxSection = new UserRolesCheckBoxSection(
            slackUserSelectSection,
            true,
            () -> slackUserSelectSection.getAccessory().isFilled()
    );

    private final MarkdownTextSection textSection = new MarkdownTextSection(
            () -> Stream.of(SlackRoles.values())
                    .map(r -> "*" + r.name() + ":* \n\t - " + Main.USER_ROLES_REPOSITORY.getAcceptedModules(r)
                            .stream()
                            .sorted()
                            .collect(Collectors.joining("\n\t - ")))
                    .collect(Collectors.joining("\n\n")),
            true,
            () -> slackUserSelectSection.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Назначение ролей";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(() -> {
            Main.USER_ROLES_REPOSITORY.clearRoles(slackUserSelectSection.getAccessory().getValue());
            userRolesCheckBoxSection.getAccessory()
                    .getValues()
                    .forEach(role ->
                            Main.USER_ROLES_REPOSITORY
                                    .add(slackUserSelectSection.getAccessory().getValue(), role)
                    );
        });
    }
}
