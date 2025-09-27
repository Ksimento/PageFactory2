package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.version_releases.Services;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleaseMessage;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleasesChannelReader;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class DockerTagFromReleasesSelectSection extends AbstractSection {
    private final Services service;

    public DockerTagFromReleasesSelectSection(
            final Services service,
            final ReleasesChannelReader reader,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(service, reader), isAndCondition, constructConditions);
        this.service = service;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText("Выбор тега " + service.getServiceName(), false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractSelectFormField {
        private final Services service;
        private final ReleasesChannelReader reader;

        Accessory(final Services service, final ReleasesChannelReader reader) {
            this.service = service;
            this.reader = reader;
        }

        @Override
        public BlockElement getElement() {
            return getCachedElement(() -> getSelect(getName(), getOptions()), service);
        }

        private List<OptionObject> getOptions() {
            final List<OptionObject> options = reader.getByService(service)
                                                     .stream()
                                                     .map(rm -> OptionObject.builder()
                                                                            .text(formatTag(rm))
                                                                            .value(rm.getVersion())
                                                                            .build()
                                                     )
                                                     .filter(ESUtils.distinctByKey(OptionObject::getValue))
                                                     .collect(Collectors.toList());
            options.add(0, OptionObject
                    .builder()
                    .value("skip")
                    .text(asText("Пропустить", false))
                    .build()
            );
            return options;
        }

        private PlainTextObject formatTag(final ReleaseMessage message) {
            return asMiddleCutText(
                    String.format(
                            "(%s) %s: %s",
                            getTagTime(message),
                            message.getEnv(),
                            message.getVersion()
                    ),
                    60,
                    false
            );
        }

        private String getTagTime(final ReleaseMessage message) {
            final LocalDateTime now = LocalDateTime.now();
            final LocalDateTime messageTs = LocalDateTime.ofEpochSecond(message.getTs(), 0, ZoneOffset.UTC);
            final Duration duration = Duration.between(messageTs, now);
            if (duration.toDays() > 1) {
                return duration.toDays() + " d";
            }
            if (duration.toHours() > 1) {
                return duration.toHours() + " h";
            }
            return duration.toMinutes() + " m";
        }

        @Override
        public String getName() {
            return "Выбор тега " + service.getServiceName();
        }
    }
}
