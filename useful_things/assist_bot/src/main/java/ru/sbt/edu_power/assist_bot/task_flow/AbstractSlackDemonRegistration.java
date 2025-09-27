package ru.sbt.edu_power.assist_bot.task_flow;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;

public abstract class AbstractSlackDemonRegistration implements SlackRegistered {
    @Override
    public LayoutBlock getStartButton() {
        return null;
    }

    @Override
    public void registerStartButton() {
        // do nothing
    }

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[0];
    }

    @Override
    public StartSection getStartSection() {
        return null;
    }

    @Override
    public String getButtonId() {
        return null;
    }
}
