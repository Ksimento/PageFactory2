package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.issue;

import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// Построение отчёта по задачам в статусах IFT, Need Test и In QA
public class IftReport {

    public List<LayoutBlock> getReport(final IssueSlice slice) {
        final List<JiraVersionModel> versionsBefore = slice.getVersionsBefore();
        versionsBefore.add(slice.getVersion());
        final List<LayoutBlock> blocks = new ArrayList<>();
        if (hasData(slice, versionsBefore)) {
            blocks.add(getIftMessage(slice));
            blocks.add(getIftIssues(slice));
        } else {
            blocks.add(getEmptyMessage(slice.getVersion()));
        }
        final List<JiraVersionModel> versionsAfter = slice.getVersionsAfter();
        if (!versionsAfter.isEmpty() && hasDataWithoutIft(slice, versionsAfter)) {
            blocks.add(DividerBlock.builder().build());
            blocks.add(getNeedTestMessage(slice));
            blocks.add(getNeedTestIssues(slice));
        }
        return blocks;
    }

    private LayoutBlock getIftMessage(final IssueSlice slice) {
        final List<JiraVersionModel> versionsBefore = slice.getVersionsBefore();
        versionsBefore.add(slice.getVersion());
        final IssueQuery issueQuery = new IssueQuery(0, 20)
                .and(
                        IssueFields.Field.ISSUE_TYPE,
                        IssueQuery.Op.IN,
                        IssueType.INCIDENT.getValue(),
                        IssueType.BUG.getValue()
                )
                .and(
                        IssueFields.Field.STATUS,
                        IssueQuery.Op.IN,
                        IssueStatus.IFT.getValue(),
                        IssueStatus.NEED_TEST.getValue(),
                        IssueStatus.IN_QA.getValue()
                )
                .and(IssueFields.Field.FIX_VERSIONS, IssueQuery.Op.IN, versionsBefore.stream().map(JiraVersionModel::getName).toArray(String[]::new))
                .and(IssueFields.Field.ASSIGNEE, IssueQuery.Op.EQUAL_FUNCTION, "currentUser()");
        final String message = String.format(
                "Коллеги, необходимо проверить <https://jira.pcbltools.ru/jira/issues/?filter=-1&jql=%s order by updated DESC|свои задачи> " +
                "в статусах %s, %s, %s в версии %s\n" +
                "Задачи в статусе %s необходимо проверять на стейдже.\n" +
                "Задачи в статусах %s и %s не должны быть в версии %s и должны быть перенесены в следующую версию.",
                issueQuery.getJql(),
                IssueStatus.IFT.getValue(),
                IssueStatus.NEED_TEST.getValue(),
                IssueStatus.IN_QA.getValue(),
                versionsBefore.stream().map(JiraVersionModel::getName).collect(Collectors.joining(", ")),
                IssueStatus.IFT.getValue(),
                IssueStatus.NEED_TEST.getValue(),
                IssueStatus.IN_QA.getValue(),
                slice.getVersion().getName()
        );

        return SectionBlock.builder().text(MarkdownTextObject.builder().text(message).build()).build();
    }

    private LayoutBlock getIftIssues(final IssueSlice slice) {
        final List<JiraVersionModel> versionsBefore = slice.getVersionsBefore();
        versionsBefore.add(slice.getVersion());
        return getSortedData(slice, versionsBefore, true);
    }

    private LayoutBlock getNeedTestMessage(final IssueSlice slice) {
        final List<JiraVersionModel> versionsAfter = slice.getVersionsAfter();
        final IssueQuery issueQuery = new IssueQuery(0, 20)
                .and(
                        IssueFields.Field.ISSUE_TYPE,
                        IssueQuery.Op.IN,
                        IssueType.INCIDENT.getValue(),
                        IssueType.BUG.getValue()
                )
                .and(
                        IssueFields.Field.STATUS,
                        IssueQuery.Op.IN,
                        IssueStatus.NEED_TEST.getValue(),
                        IssueStatus.IN_QA.getValue()
                )
                .and(IssueFields.Field.FIX_VERSIONS, IssueQuery.Op.IN, versionsAfter.stream().map(JiraVersionModel::getName).toArray(String[]::new))
                .and(IssueFields.Field.ASSIGNEE, IssueQuery.Op.EQUAL_FUNCTION, "currentUser()");
        final String message = String.format(
                "Коллеги, необходимо проверить <https://jira.pcbltools.ru/jira/issues/?filter=-1&jql=%s order by updated DESC|свои задачи> " +
                "в статусах %s и %s в версии %s и перевести их в Need Merge " +
                "если дефекты исправлены.",
                issueQuery.getJql(),
                IssueStatus.NEED_TEST.getValue(),
                IssueStatus.IN_QA.getValue(),
                versionsAfter.stream().map(JiraVersionModel::getName).collect(Collectors.joining(", "))
        );
        return SectionBlock.builder().text(MarkdownTextObject.builder().text(message).build()).build();
    }

    private LayoutBlock getNeedTestIssues(final IssueSlice slice) {
        final List<JiraVersionModel> versionsAfter = slice.getVersionsAfter();
        return getSortedData(slice, versionsAfter, false);
    }

    private LayoutBlock getSortedData(
            final IssueSlice slice,
            final List<JiraVersionModel> versions,
            final boolean withIft
    ) {
        final Map<JiraUser, List<IssueFields>> data = slice.getUserToIssueListByVersions(versions);
        final Map<JiraUser, EnumMap<IssueStatus, List<IssueFields>>> dataByStatus = new HashMap<>();
        data.forEach((user, list) -> {
            list.forEach(issue -> {
                final IssueStatus status = IssueStatus.getByValue(issue.get(IssueFields.Field.STATUS));
                if (!withIft) {
                    if (status == IssueStatus.IFT) {
                        return;
                    }
                }
                if (!dataByStatus.containsKey(user)) {
                    dataByStatus.put(user, new EnumMap<>(IssueStatus.class));
                }
                if (!dataByStatus.get(user).containsKey(status)) {
                    dataByStatus.get(user).put(status, new ArrayList<>());
                }
                dataByStatus.get(user).get(status).add(issue);
            });
        });
        final String report = dataByStatus.entrySet()
                                          .stream()
                                          .map(e -> {
                                              final String issues = e.getValue()
                                                                     .entrySet()
                                                                     .stream()
                                                                     .map(es -> String.format(
                                                                             "%s (%d)",
                                                                             es.getKey().getValue(),
                                                                             es.getValue().size()
                                                                     ))
                                                                     .collect(Collectors.joining("; "));
                                              return String.format("*%s*: %s", e.getKey().getDisplayName(), issues);
                                          })
                                          .collect(Collectors.joining("\n"));
        return SectionBlock.builder()
                           .text(MarkdownTextObject.builder().text(report).build())
                           .build();
    }

    private LayoutBlock getEmptyMessage(final JiraVersionModel version) {
        final String message = String.format(
                "Для версии %s нет актуальных задач",
                version.getName()
        );
        return SectionBlock.builder().text(MarkdownTextObject.builder().text(message).build()).build();
    }

    private boolean hasData(final IssueSlice slice, final List<JiraVersionModel> versions) {
        return slice.getUserToIssueListByVersions(versions).values()
                    .stream()
                    .mapToLong(List::size)
                    .sum() > 0;
    }

    private boolean hasDataWithoutIft(final IssueSlice slice, final List<JiraVersionModel> versions) {
        return slice.getUserToIssueListByVersions(versions).values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(is -> Objects.nonNull(is.get(IssueFields.Field.STATUS)))
                    .anyMatch(is -> !Objects
                            .requireNonNull(is.get(IssueFields.Field.STATUS))
                            .equals(IssueStatus.IFT.getValue()));
    }
}
