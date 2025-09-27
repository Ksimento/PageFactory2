package ru.sbt.edu_power.assist_bot.tasks.releases.master_version.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.external_services.version_releases.ReleaseProjectId;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReleaseProjectSelectSection extends AbstractSection {

    public ReleaseProjectSelectSection() {
        super(new Accessory());
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText("Выбрать название проекта", false))
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractSelectFormField {

        @Override
        public BlockElement getElement() {
            return getSelect("Проект", getOptions());
        }

        // В значениях хранятся name() от енума сервиса
        private List<OptionObject> getOptions() {
            return Stream.of(ReleaseProjectId.values())
                                                     .filter(s -> s != ReleaseProjectId.UNDEFINED)
                                                     .map(s -> OptionObject
                                                             .builder()
                                                             .text(asText(s.getService(), false))
                                                             .value(s.name())
                                                             .build()
                                                     )
                                                     .collect(Collectors.toList());
        }

        @Override
        public String getName() {
            return "Выбор проекта";
        }
    }
}
