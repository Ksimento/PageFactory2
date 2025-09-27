package ru.sbt.edu_power.assist_bot.services.channel_reader;

import com.slack.api.model.Attachment;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.version_releases.Services;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ReleasesChannelReader {
    private final List<ReleaseMessage> messages = new ArrayList<>();
    private final ChannelReader channelReader;

    public ReleasesChannelReader(final String userId) {
        channelReader = new ChannelReader(SlackClient.getChannelId(Channels.RELEASES.channelName), userId, 800);
    }

    public ReleasesChannelReader(final String userId, final Channels channel, final int limit) {
        channelReader = new ChannelReader(SlackClient.getChannelId(channel.channelName), userId, limit);
    }

    public void updateMessages() {
        messages.clear();
        channelReader.waitWhenChanelBeRead();
        channelReader.getMessages().forEach(m -> {
            final ReleaseMessage releaseMessage = new ReleaseMessage(m.getTs());
            final String message;
            if (Objects.isNull(m.getAttachments())) {
                message = m.getText();
            } else {
                message = m.getAttachments()
                           .stream()
                           .map(Attachment::getFallback)
                           .collect(Collectors.joining("\n"));
            }
            final String symbol160 = new String(new char[]{160});
            releaseMessage.parse(
                    message.replaceAll("&gt; ", "")
                           .replaceAll("`", "")
                           .replaceAll("\\*", "")
                           .replaceAll(symbol160, " ")
            );
            if (
                    Objects.nonNull(releaseMessage.getService()) &&
                    Objects.nonNull(releaseMessage.getVersion())
            ) {
                messages.add(releaseMessage);
            }
        });
    }

    public void removeUnsuccessfulServices() {
        messages.removeIf(m -> Objects.nonNull(m.getStatus()) && m.getStatus() != ReleaseMessage.Status.SUCCESS);
    }

    public List<ReleaseMessage> getByService(final Services serviceName) {
        return messages.stream()
                       .filter(m -> m.getService() == serviceName)
                       .collect(Collectors.toList());
    }

    public List<ReleaseMessage> getByStand(final String stand) {
        return messages.stream()
                       .filter(m -> m.getEnv().equals(stand))
                       .collect(Collectors.toList());
    }

    public List<ReleaseMessage> getMessages() {
        return messages;
    }

    public List<ReleaseMessage> getByStandAndService(final String stand, final Services services) {
        return getByService(services)
                .stream()
                .filter(m -> m.getEnv().equals(stand))
                .collect(Collectors.toList());
    }

    public List<ReleaseMessage> getStartServices() {
        return messages.stream()
                       .filter(ReleaseMessage::isStartService)
                       .collect(Collectors.toList());
    }

    public List<ReleaseMessage> getByBuildId(final String buildId) {
        return messages.stream()
                       .filter(m -> buildId.equals(m.getBuildId()))
                       .filter(m -> !m.isStartService())
                       .collect(Collectors.toList());
    }

    public List<String> getEnvList() {
        return messages.stream()
                       .map(ReleaseMessage::getEnv)
                       .distinct()
                       .collect(Collectors.toList());
    }

    public enum Channels {
        RELEASES("releases"),
        MAINTENANCE("maintenance");
        private final String channelName;

        Channels(final String channelName) {
            this.channelName = channelName;
        }

        public String getChannelName() {
            return channelName;
        }
    }
}
