package ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.external_services.version_releases.Services;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleaseMessage;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleasesChannelReader;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;

import java.util.List;
import java.util.stream.Collectors;

public class StandByServiceSelectSection extends AbstractSection {

    public StandByServiceSelectSection(
            final ReleasesServicesSelectSection releaseSelect,
            final ReleasesChannelReader reader
    ) {
        super(new Accessory(releaseSelect, reader));
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText("Выбери стенд для получения установленной версии", false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractSelectFormField {
        private final ReleasesServicesSelectSection releaseSelect;
        private final ReleasesChannelReader reader;

        Accessory(
                final ReleasesServicesSelectSection releaseSelect,
                final ReleasesChannelReader reader
        ) {
            this.releaseSelect = releaseSelect;
            this.reader = reader;
        }

        @Override
        public BlockElement getElement() {
            return getSelect("Выбор стенда", getOptions());
        }

        private List<OptionObject> getOptions() {
            final List<String> stands;
            if (releaseSelect.getAccessory().isFilled()) {
                stands = reader.getByService(Services.valueOf(releaseSelect.getAccessory().getValue()))
                               .stream()
                               .map(ReleaseMessage::getEnv)
                               .distinct()
                               .collect(Collectors.toList());
            } else {
                stands = reader.getEnvList();
            }
            final List<OptionObject> options = stands
                    .stream()
                    .map(stand -> OptionObject.builder()
                                              .text(asText(stand, false))
                                              .value(stand)
                                              .build()
                    )
                    .collect(Collectors.toList());
            options.add(0, OptionObject.builder().text(asText("Без указания стенда", false)).value("none").build());
            return options;
        }

        @Override
        public String getName() {
            return null;
        }
    }
}
