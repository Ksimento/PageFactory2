package ru.sbt.edu_power.assist_bot.slack.views;

import com.slack.api.model.block.SectionBlock;

public interface Section {
    boolean constructCondition();
    SectionBlock construct();
    FormField getAccessory();
    String getId();
}
