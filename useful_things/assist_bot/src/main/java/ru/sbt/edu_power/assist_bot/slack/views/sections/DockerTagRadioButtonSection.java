package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.services.jenkins.DockerTagSearch;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractRadioButtonFormField;
import ru.sbt.edu_power.assist_bot.tasks.full_deploy.FullDeployUtils;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Секция для модалки с возможностью выбора докер-тега
 * Требует указания версии приложения из джиры, репозитория, проектной области джиры
 */
public class DockerTagRadioButtonSection extends AbstractSection {
    private final String text;

    public DockerTagRadioButtonSection(
            final Supplier<String> branch,
            final DockerTagSearch.Repo repo,
            final String text
    ) {
        super(new Accessory(branch, repo));
        this.text = text;
    }

    public DockerTagRadioButtonSection(
            final Supplier<String> branch,
            final DockerTagSearch.Repo repo,
            final String text,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(branch, repo), isAndCondition, constructConditions);
        this.text = text;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText(text, false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractRadioButtonFormField {
        private final Supplier<String> branch;
        private final DockerTagSearch.Repo repo;

        Accessory(
                final Supplier<String> branch,
                final DockerTagSearch.Repo repo
        ) {
            this.branch = branch;
            this.repo = repo;
        }

        @Override
        public BlockElement getElement() {
            return getCachedElement(() -> getRadioButtons(getOptions()), repo, branch.get());
        }

        private List<OptionObject> getOptions() {
            final List<OptionObject> options = FullDeployUtils.getTagOptions(repo, branch.get(), false);
            if (options.isEmpty()) {
                options.add(OptionObject
                        .builder()
                        .text(asText(":no_bicycles: Подходящие теги не найдены", true))
                        .value("none")
                        .build());
            }
            return options;
        }

        @Override
        public String getName() {
            return "Выбор доступного докер-тега";
        }
    }
}
