package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.AnalyticUtils;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.assist_bot.task_flow.Container;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Селект для выбора сотрудника из списка участников регресса с указанием количества не пройденных тест-кейсов
public class TestCaseUserAssignedSelectSection extends AbstractSection {
    private final String name;

    public TestCaseUserAssignedSelectSection(
            final String name,
            final Container<TestRunSlice> testRunSliceContainer,
            final boolean isFromUser
    ) {
        super(new Accessory(name, testRunSliceContainer, isFromUser));
        this.name = name;
    }

    public TestCaseUserAssignedSelectSection(
            final String name,
            final Container<TestRunSlice> testRunSliceContainer,
            final boolean isFromUser,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(name, testRunSliceContainer, isFromUser), isAndCondition, constructConditions);
        this.name = name;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .text(asText(name, true))
                           .build();
    }

    private static class Accessory extends AbstractSelectFormField {
        private final String name;
        private final Container<TestRunSlice> testRunSliceContainer;
        // true для селекта со списком доноров (от кого перекидываем). В этом случае фильтруем по убыванию и исключаем пользователей без тест-кейсов
        private final boolean isFromUser;

        Accessory(
                final String name,
                final Container<TestRunSlice> testRunSliceContainer,
                final boolean isFromUser
        ) {
            this.name = name;
            this.testRunSliceContainer = testRunSliceContainer;
            this.isFromUser = isFromUser;
        }

        @Override
        public BlockElement getElement() {
            return getSelect(name, getOptions());
        }

        @Override
        public String getName() {
            return name;
        }

        private List<OptionObject> getOptions() {
            final Stream<Map.Entry<JiraUser, Map<String, List<Execution>>>> stream =
                    testRunSliceContainer
                            .getObject()
                            .getUserToExecutionStatusToExecutionsMap()
                            .entrySet()
                            .stream();
            final Stream<Map.Entry<JiraUser, Map<String, List<Execution>>>> streamSorted;

            if (isFromUser) {
                streamSorted = stream
                        .filter(e -> getTotal(e.getValue()) != 0)
                        .sorted((a, b) -> getTotal(b.getValue()).compareTo(getTotal(a.getValue())));
            } else {
                streamSorted = stream.sorted(Comparator.comparing(a -> getTotal(a.getValue())));
            }

            final List<OptionObject> list = streamSorted
                    .map(e -> getOption(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            return list;
        }

        private OptionObject getOption(
                final JiraUser user,
                final Map<String, List<Execution>> executions
        ) {
            final int total = getTotal(executions);
            return OptionObject.builder()
                               .text(asText(String.format("(%d) %s", total, user.getDisplayName()), false))
                               .value(user.getName())
                               .build();
        }

        private Integer getTotal(final Map<String, List<Execution>> executions) {
            return executions.keySet()
                             .stream()
                             .filter(AnalyticUtils::isInNotStartedStatus)
                             .map(executions::get)
                             .mapToInt(List::size)
                             .sum();
        }
    }
}
