package ru.sbt.edu_power.assist_bot.tasks.test_run.test_run_creation;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class TestRunCreationRegistration extends AbstractSlackRegistration {

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.REGRESS};
    }

    @Override
    public String getName() {
        return "Создание тест-сета";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.REGRESS;
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton(
                "Создать тест-сет",
                "Создание тест-сета по предустановленным фильтрам"
        );
    }

    @Override
    public void registerStartButton() {
        registerStartButton(TestRunCreationView::new);
    }
}
