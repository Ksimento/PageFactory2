package ru.sbt.edu_power.assist_bot.tasks.releases.last_releases;

import ru.sbt.edu_power.external_services.version_releases.Release;
import ru.sbt.edu_power.external_services.version_releases.Releases;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.users.SlackUsers;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LastReleasesDispatcher implements IDispatcher {
    private final LastReleasesView view;

    public LastReleasesDispatcher(final LastReleasesView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        final Releases releases = new Releases();
        sendData(releases.getReleaseByServiceMap());
    }

    private void sendData(final Map<String, List<Release>> releaseByServiceMap) {
        final String message = releaseByServiceMap
                .entrySet()
                .stream()
                .map(e -> {
                    final String releaseName = e.getKey();
                    final String data = e.getValue()
                                         .stream()
                                         .sorted()
                                         .map(r -> r.toString(SlackUsers
                                                 .getInstance()
                                                 .getUserTz(view.getUserId())))
                                         .map(s -> ">" + s)
                                         .collect(Collectors.joining("\n"));
                    return releaseName + "\n" + data;
                })
                .collect(Collectors.joining("\n"));
        SlackClient.sendText(message, view.getUserId());
    }

}
