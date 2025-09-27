package ru.sbt.edu_power.assist_bot.tasks.releases.master_version;

import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.external_services.version_releases.MasterVersion;
import ru.sbt.edu_power.external_services.version_releases.ReleaseProjectId;

public class MasterVersionDispatcher implements IDispatcher {
    private final MasterVersionView view;

    public MasterVersionDispatcher(final MasterVersionView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        final MasterVersion masterVersion = new MasterVersion();
        final ReleaseProjectId releaseProjectId = ReleaseProjectId.valueOf(view.getReleaseProjectSelectSection().getAccessory().getValue());
        SlackClient.sendText(
                String.format("Текущая версия мастера для проекта '%s': %s", releaseProjectId.getService(), masterVersion.getMasterVersion(releaseProjectId)),
                view.getUserId()
        );
    }
}
