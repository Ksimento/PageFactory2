package ru.sbt.edu_power.assist_bot.tasks.test_run.regress;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class RegressStartRegistration extends AbstractSlackRegistration {
    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.REGRESS};
    }

    @Override
    public String getName() {
        return "Запуск регресса";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.REGRESS;
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton(
                "Запуск регресса",
                "Сценарий выполняет деплой стендов, создание тест-сета, прогон UI АФТ, контроль прохождения регресса"
                );
    }

    @Override
    public void registerStartButton() {
        registerStartButton(RegressStartView::new);
    }
}
