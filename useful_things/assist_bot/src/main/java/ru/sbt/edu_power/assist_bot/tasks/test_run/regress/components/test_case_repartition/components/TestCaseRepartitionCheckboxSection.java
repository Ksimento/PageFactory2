package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.TestCaseRepartition;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.TestCaseRepartitionUtils;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModelFolder;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractCheckBoxFormField;
import ru.sbt.edu_power.assist_bot.task_flow.Container;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// список переключателей для выбора директории с тест-кейсами для передачи их прохождения другому сотруднику
public class TestCaseRepartitionCheckboxSection extends AbstractSection {

    public TestCaseRepartitionCheckboxSection(
            final String name,
            final Supplier<JiraUser> jiraUserSupplier,
            final Container<TestRunSlice> testRunSliceContainer,
            final Container<TestCaseRepartition> testCaseRepartitionContainer
    ) {
        super(new Accessory(name, jiraUserSupplier, testRunSliceContainer, testCaseRepartitionContainer));
    }

    public TestCaseRepartitionCheckboxSection(
            final String name,
            final Supplier<JiraUser> jiraUserSupplier,
            final Container<TestRunSlice> testRunSliceContainer,
            final Container<TestCaseRepartition> testCaseRepartitionContainer,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(
                new Accessory(name, jiraUserSupplier, testRunSliceContainer, testCaseRepartitionContainer),
                isAndCondition,
                constructConditions
        );
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText("Выбери один из предложенных наборов тест-кейсов", false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractCheckBoxFormField {
        private final String name;
        private final Supplier<JiraUser> jiraUserSupplier;
        private final Container<TestRunSlice> testRunSliceContainer;
        private final Container<TestCaseRepartition> testCaseRepartitionContainer;

        Accessory(
                final String name,
                final Supplier<JiraUser> jiraUserSupplier,
                final Container<TestRunSlice> testRunSliceContainer,
                final Container<TestCaseRepartition> testCaseRepartitionContainer
        ) {
            this.name = name;
            this.jiraUserSupplier = jiraUserSupplier;
            this.testRunSliceContainer = testRunSliceContainer;
            this.testCaseRepartitionContainer = testCaseRepartitionContainer;
        }

        @Override
        public BlockElement getElement() {
            return getCheckBoxList(getOptions());
        }

        @Override
        public String getName() {
            return name;
        }

        private List<OptionObject> getOptions() {
            testCaseRepartitionContainer
                    .setObject(new TestCaseRepartition(
                            testRunSliceContainer.getObject(),
                            jiraUserSupplier.get()
                    ));
            return testCaseRepartitionContainer.getObject().getFunctionalParts().entrySet()
                                               .stream()
                                               .sorted(Comparator.comparing(a -> TestCaseRepartitionUtils.getFolderPath(
                                                       a.getKey())))
                                               .map(e -> getOption(e.getKey(), e.getValue()))
                                               .limit(10)
                                               .collect(Collectors.toList());
        }

        private OptionObject getOption(
                final TestCaseModelFolder folder,
                final List<Execution> executions
        ) {
            return OptionObject.builder()
                               .text(asMiddleCutText(String.format(
                                       "(%d) %s",
                                       executions.size(),
                                       TestCaseRepartitionUtils.getFolderPath(folder)
                               ), 150, false))
                               .value(folder.getId().toString())
                               .build();
        }

    }
}
