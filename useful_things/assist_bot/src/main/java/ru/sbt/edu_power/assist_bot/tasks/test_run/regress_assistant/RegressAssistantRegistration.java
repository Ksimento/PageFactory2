package ru.sbt.edu_power.assist_bot.tasks.test_run.regress_assistant;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class RegressAssistantRegistration extends AbstractSlackRegistration {
    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.REGRESS};
    }

    @Override
    public String getName() {
        return "Ассистент регресса";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.REGRESS;
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Ассистент регресса", "Запуск модуля, ассистирующего регресс");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(RegressAssistantView::new);
    }
}
