package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.services.jenkins.DockerTagSearch;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractHasExternalLoadFormField;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.tasks.full_deploy.FullDeployUtils;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Секция для модалки, позволяет через селект с подсказками выбрать последний докер-тег по названию ветки
 * Требует указания репозитория, в котором находится ветка
 * Докер тег обнаруживается автоматически в мультибранч джобах соответствующих репозиториев
 */
@Slf4j
public class DockerTagSelectSection extends AbstractSection {
    private final DockerTagSearch.Repo repo;

    public DockerTagSelectSection(final DockerTagSearch.Repo repo) {
        super(new Accessory(repo));
        this.repo = repo;
    }

    public DockerTagSelectSection(
            final DockerTagSearch.Repo repo,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(repo), isAndCondition, constructConditions);
        this.repo = repo;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText(
                                   "Введите номер задачи или часть названия ветки чтобы найти доступный тег " + repo.name(),
                                   false
                           ))
                           .accessory(getAccessory().getElement())
                           .blockId(getId())
                           .build();
    }

    private static class Accessory extends AbstractHasExternalLoadFormField {
        private final String name;
        private final DockerTagSearch.Repo repo;

        Accessory(final DockerTagSearch.Repo repo) {
            this.repo = repo;
            name = repo.name() + " docker tag";
        }

        @Override
        public BlockElement getElement() {
            return getSelect(name, 5);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void callback() {
            APP.blockSuggestion(getId(), (req, ctx) -> {
                final List<Option> optionList = FullDeployUtils.getTagSuggestionOptions(
                        repo,
                        req.getPayload().getValue()
                );
                return ctx.ack(r -> r.options(optionList));
            });
        }
    }
}
