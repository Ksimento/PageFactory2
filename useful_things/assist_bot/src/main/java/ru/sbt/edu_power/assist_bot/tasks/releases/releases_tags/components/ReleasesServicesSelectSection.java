package ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.external_services.version_releases.Services;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReleasesServicesSelectSection extends AbstractSection {

    public ReleasesServicesSelectSection() {
        super(new Accessory());
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText("Выбрать название сервиса", false))
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractSelectFormField {

        @Override
        public BlockElement getElement() {
            return getSelect("Сервис", getOptions());
        }

        // В значениях хранятся name() от енума сервиса
        private List<OptionObject> getOptions() {
            final List<OptionObject> options = Stream.of(Services.values())
                                                     .filter(s -> s != Services.OTHER)
                                                     .map(s -> OptionObject
                                                             .builder()
                                                             .text(asText(s.getServiceName(), false))
                                                             .value(s.name())
                                                             .build()
                                                     )
                                                     .collect(Collectors.toList());
            options.add(0, OptionObject.builder().text(asText("Без указания версии", false)).value("none").build());
            return options;
        }

        @Override
        public String getName() {
            return "Выбор сервиса";
        }
    }
}
