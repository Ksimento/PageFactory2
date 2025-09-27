package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasProjectAndVersions;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.RegressStartView;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.TestRunSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractRadioButtonFormField;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// Выбор тест-сета, в виде RADIO
// в ключе передаётся ключ тест-сета
public class TestRunSelectSection extends AbstractSection {

    public TestRunSelectSection(final IHasProjectAndVersions view) {
        super(new Accessory(view));
    }

    public TestRunSelectSection(
            final IHasProjectAndVersions view,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(view), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .text(asText("Или выбери уже существующий тест-сет", false))
                           .build();
    }

    private static class Accessory extends AbstractRadioButtonFormField {
        private final IHasProjectAndVersions view;

        Accessory(final IHasProjectAndVersions view) {
            this.view = view;
        }

        //
        @Override
        public BlockElement getElement() {
            final Supplier<BlockElement> elementSupplier = () -> {
                final List<OptionObject> options = getOptions();
                if (options.isEmpty()) {
                    return getRadioButtons(Collections.singletonList(
                            OptionObject.builder()
                                        .text(asText("Нет созданных тест-сетов", false))
                                        .value("none")
                                        .build()
                    ));
                }
                return getRadioButtons(options);
            };
            return getCachedElement(
                    elementSupplier,
                    view.getJiraProjectId().name(),
                    view.getTestRunVersion().getId()
            );
        }

        private List<OptionObject> getOptions() {
            JiraVersionModel version;
            try {
                version = view.getTestRunVersion();
            } catch (final Exception e) {
                version = null;
            }
            final List<OptionObject> options = new ArrayList<>();
            if (Objects.nonNull(version)) {
                options.addAll(
                        new TestRunSearch(version, view.getJiraProjectId())
                                .getTestRunList()
                                .stream()
                                .sorted((a, b) -> b.getTestCaseCount().compareTo(a.getTestCaseCount()))
                                .map(t -> OptionObject.builder()
                                                      .text(asText(
                                                              formatName(t),
                                                              false
                                                      ))
                                                      .value(t.getKey())
                                                      .build()
                                )
                                .limit(10)
                                .collect(Collectors.toList()));
            }
            return options;
        }

        private String formatName(final TestRunModel testRunModel) {
            final String name = testRunModel.getName().substring(0, Math.min(testRunModel.getName().length(), 110));
            return String.format("%s ID: %s Тестов: %d", name, testRunModel.getKey(), testRunModel.getTestCaseCount());
        }

        @Override
        public String getName() {
            return "Список уже созданных тест-сетов";
        }
    }
}
