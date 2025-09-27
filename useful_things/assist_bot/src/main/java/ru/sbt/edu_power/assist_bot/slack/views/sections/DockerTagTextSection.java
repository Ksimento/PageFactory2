package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

/**
 * Секция для модалки для вывода полного названия докер-тега, который был выбран в секции DockerTagSelectSection
 */
public class DockerTagTextSection extends AbstractSection {
    private final DockerTagSelectSection section;
    private final String name;

    public DockerTagTextSection(final DockerTagSelectSection section, final String name) {
        super(null);
        this.section = section;
        this.name = name;
    }

    @Override
    public boolean constructCondition() {
        return section.getAccessory().isFilled();
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText(name + section.getAccessory().getValue(), false))
                           .build();
    }
}
