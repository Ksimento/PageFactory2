package ru.sbt.edu_power.assist_bot.tasks.test_run.remove_tests;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class RemoveTestsRegistration extends AbstractSlackRegistration {

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.REGRESS};
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Удалить тесты",
                "Удалить все непройденные тесты из тест-сета. Полезно для тест-сетов созданных под автотесты"
        );
    }

    @Override
    public void registerStartButton() {
        registerStartButton(RemoveTestsView::new);
    }

    @Override
    public String getName() {
        return "Удаление непройденных тест-кейсов";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.REGRESS;
    }
}
