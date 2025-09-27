package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// сохраняет название репозитория repoName из BBRepos
public class BBRepoSelectSection extends AbstractSection {

    public BBRepoSelectSection() {
        super(new Accessory());
    }

    public BBRepoSelectSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                .blockId(getId())
                .accessory(getAccessory().getElement())
                .text(asText("Репозиторий в проекте", false))
                .build();
    }

    private static class Accessory extends AbstractSelectFormField {

        @Override
        public BlockElement getElement() {
            return getSelect("Репозиторий", getOptions());
        }

        private List<OptionObject> getOptions() {
            return Stream.of(BBRepos.values())
                         .map(v -> OptionObject.builder()
                                               .text(asText(v.name(), false))
                                               .value(v.getRepoName())
                                               .build()
                         )
                         .collect(Collectors.toList());
        }

        @Override
        public String getName() {
            return "Репозиторий";
        }
    }
}
