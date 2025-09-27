package ru.sbt.edu_power.assist_bot.tasks.releases.last_releases;

import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.TextSection;

public class LastReleasesView extends AbstractModal {
    private final TextSection text = new TextSection(
            () -> "Эта форма не требует дополнительных настроек"
    );

    @Override
    public String getName() {
        return "Последние релизы";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new LastReleasesDispatcher(this));
    }
}
