package ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleasesChannelReader;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags.components.ReleasesServicesSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags.components.ReleasesTagListSection;
import ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags.components.StandByServiceSelectSection;

@Getter
public class ReleasesTagsView extends AbstractModal {
    private final ReleasesChannelReader reader = new ReleasesChannelReader(getUserId());

    private final ReleasesServicesSelectSection releasesServicesSelectSection = new ReleasesServicesSelectSection();

    private final StandByServiceSelectSection standByServiceSelectSection = new StandByServiceSelectSection(
            releasesServicesSelectSection,
            reader
    );

    private final ReleasesTagListSection releasesTagListSection = new ReleasesTagListSection(
            releasesServicesSelectSection,
            standByServiceSelectSection,
            reader
    );

    public ReleasesTagsView() {
        reader.updateMessages();
        reader.removeUnsuccessfulServices();
    }

    @Override
    public String getName() {
        return "Поиск тегов Releases";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(
                () -> SlackClient.sendText(releasesTagListSection.getAccessory().getValue(), getUserId())
        );
    }
}
