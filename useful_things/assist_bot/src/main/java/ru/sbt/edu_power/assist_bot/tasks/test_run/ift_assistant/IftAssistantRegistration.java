package ru.sbt.edu_power.assist_bot.tasks.test_run.ift_assistant;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class IftAssistantRegistration extends AbstractSlackRegistration {
    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("IFT ассистент", "Ассистент проконтроллирует тестирование задач в статусе IFT");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(IftAssistantView::new);
    }

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.REGRESS};
    }

    @Override
    public String getName() {
        return "IFT ассистент";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.REGRESS;
    }
}
