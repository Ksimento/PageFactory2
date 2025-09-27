package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.regress_control;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.TextSection;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class RegressControlView extends AbstractModal {
    private final AtomicBoolean generateFinalReport;

    private final TextSection noPermissionSection = new TextSection(
            () -> "Для выполнения действий над регрессионным прогоном у вас не достаточно прав",
            true,
            () -> !Main.USER_ROLES_REPOSITORY.hasAny(this.getUserId(), SlackRoles.REGRESS, SlackRoles.ADMIN)
    );

    private final CreateReportButton createReportButton;

    public RegressControlView(final AtomicBoolean generateFinalReport) {
        this.generateFinalReport = generateFinalReport;
        createReportButton = new CreateReportButton(
                generateFinalReport,
                true,
                () -> Main.USER_ROLES_REPOSITORY.hasAny(this.getUserId(), SlackRoles.REGRESS, SlackRoles.ADMIN)
        );
    }

    @Override
    public String getName() {
        return "Контроль регресса";
    }

    @Override
    public void registerViewSubmit() {

    }
}
