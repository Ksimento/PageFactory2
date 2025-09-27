package ru.sbt.edu_power.assist_bot.tasks.releases.master_version;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.tasks.releases.master_version.components.ReleaseProjectSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags.components.ReleasesServicesSelectSection;
import ru.sbt.edu_power.external_services.version_releases.MasterVersion;

@Getter
public class MasterVersionView extends AbstractModal {
    private final ReleaseProjectSelectSection releaseProjectSelectSection = new ReleaseProjectSelectSection();

    @Override
    public String getName() {
        return "Версия мастера";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new MasterVersionDispatcher(this));
    }
}
