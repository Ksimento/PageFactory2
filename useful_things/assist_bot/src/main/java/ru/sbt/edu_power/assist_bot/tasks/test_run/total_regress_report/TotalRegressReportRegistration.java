package ru.sbt.edu_power.assist_bot.tasks.test_run.total_regress_report;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class TotalRegressReportRegistration extends AbstractSlackRegistration {

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.REGRESS};
    }

    @Override
    public String getName() {
        return "Итоговый отчёт по регрессу";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.REGRESS;
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton(
                "Итоговый отчёт",
                "Генерация итогового отчёта о прохождении регресса"
        );
    }

    @Override
    public void registerStartButton() {
        registerStartButton(TotalRegressReportView::new);
    }
}
