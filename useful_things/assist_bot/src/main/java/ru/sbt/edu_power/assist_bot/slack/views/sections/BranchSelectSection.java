package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractHasExternalLoadFormField;
import ru.sbt.edu_power.external_services.bitbucket.BBConnection;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class BranchSelectSection extends AbstractSection {
    private final BBRepos repo;

    public BranchSelectSection(
            final BBRepos repo
    ) {
        super(new Accessory(repo));
        this.repo = repo;
    }

    public BranchSelectSection(
            final BBRepos repo,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(repo), isAndCondition, constructConditions);
        this.repo = repo;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .text(asText("Выбор ветки из репозитория " + repo.getRepoName(), false))
                           .build();
    }

    private static class Accessory extends AbstractHasExternalLoadFormField {
        private final BBRepos repo;

        Accessory(final BBRepos repo) {
            this.repo = repo;
        }

        @Override
        public BlockElement getElement() {
            return getSelect(repo.getRepoName(), 5);
        }

        @Override
        public String getName() {
            return "Выбор ветки";
        }

        @Override
        public void callback() {
            APP.blockSuggestion(getId(), (req, ctx) -> {
                final List<BranchModel> branches = BBConnection.getBranches(
                        repo,
                        req.getPayload().getValue()
                );
                final List<Option> optionList = branches.stream()
                                                        .map(b -> Option
                                                                .builder()
                                                                .text(asText(limitName(b.getDisplayId()), false))
                                                                .value(b.getDisplayId())
                                                                .build())
                                                        .limit(15)
                                                        .collect(Collectors.toList());
                return ctx.ack(r -> r.options(optionList));
            });
        }

        private String limitName(final String name) {
            return name.substring(0, Math.min(name.length(), 60));
        }
    }
}
