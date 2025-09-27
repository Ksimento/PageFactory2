package ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.view.ViewState;
import ru.sbt.edu_power.external_services.version_releases.Services;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleaseMessage;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleasesChannelReader;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractFormField;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReleasesTagListSection extends AbstractSection {

    public ReleasesTagListSection(
            final ReleasesServicesSelectSection service,
            final StandByServiceSelectSection stand,
            final ReleasesChannelReader reader
    ) {
        super(new Accessory(service, stand, reader));
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asMrkdn(getAccessory().getValue()))
                           .build();
    }

    private static class Accessory extends AbstractFormField {
        private final ReleasesServicesSelectSection service;
        private final StandByServiceSelectSection stand;
        private final ReleasesChannelReader reader;
        private static final LocalDateTime CURRENT = LocalDateTime.now();

        Accessory(
                final ReleasesServicesSelectSection service,
                final StandByServiceSelectSection stand,
                final ReleasesChannelReader reader
        ) {
            this.service = service;
            this.stand = stand;
            this.reader = reader;
        }

        @Override
        public void setState(final ViewState.Value value) {
            // do nothing
        }

        @Override
        public BlockElement getElement() {
            return null;
        }

        @Override
        public String getName() {
            return "Список тегов";
        }

        @Override
        public String getValue() {
            setValue(getTagsList());
            return super.getValue();
        }

        private String getTagsList() {
            final List<ReleaseMessage> messageList;
            if (service.getAccessory().isFilled()) {
                final Services serviceEnum = Services.valueOf(service.getAccessory().getValue());
                if (stand.getAccessory().isFilled()) {
                    messageList = reader.getByStandAndService(
                            stand.getAccessory().getValue(),
                            serviceEnum
                    );
                } else {
                    messageList = reader.getByService(serviceEnum);
                }
            } else {
                if (stand.getAccessory().isFilled()) {
                    messageList = reader.getByStand(stand.getAccessory().getValue());
                } else {
                    messageList = reader.getMessages().stream()
                                        .sorted()
                                        .limit(50)
                                        .collect(Collectors.toList());
                }
            }
            if (messageList.isEmpty()) {
                return "Нет данных для отображения";
            }
            final Map<String, List<ReleaseMessage>> map = new LinkedHashMap<>();
            messageList.stream()
                       .sorted()
                       .forEach(rm -> {
                           if (!map.containsKey(rm.getEnv())) {
                               map.put(rm.getEnv(), new ArrayList<>());
                           }
                           map.get(rm.getEnv()).add(rm);
                       });
            final String result = map.entrySet()
                      .stream()
                      .map(this::formatTagsByStand)
                      .collect(Collectors.joining("\n\n"));
            if (result.length() > 3000) {
                return result.substring(0, 2990) + "....";
            }
            return result;
        }

        private String formatReleaseMessage(final ReleaseMessage message) {
            final LocalDateTime date = LocalDateTime.ofEpochSecond(message.getTs(), 0, ZoneOffset.ofHours(3));
            return String.format(
                    "\t(%s) _%s_: *%s*",
                    formatDuration(Duration.between(date, CURRENT)),
                    message.getService().getServiceName(),
                    message.getVersion()
            );
        }

        private String formatTagsByStand(final Map.Entry<String, List<ReleaseMessage>> messages) {
            return String.format(
                    "*%s*\n%s",
                    messages.getKey(),
                    messages.getValue()
                            .stream()
                            .map(this::formatReleaseMessage)
                            .collect(Collectors.joining("\n"))
            );
        }

        private String formatDuration(final Duration duration) {
            if (duration.toDays() > 1) {
                return duration.toDays() + " d";
            }
            if (duration.toHours() > 1) {
                return duration.toHours() + " h";
            }
            return duration.toMinutes() + " m";
        }
    }


}
