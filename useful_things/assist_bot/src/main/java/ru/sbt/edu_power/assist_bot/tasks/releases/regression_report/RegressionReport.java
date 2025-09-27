package ru.sbt.edu_power.assist_bot.tasks.releases.regression_report;

import com.slack.api.model.Message;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import ru.sbt.edu_power.assist_bot.reporting.ReportSearch;
import ru.sbt.edu_power.assist_bot.reporting.ReportType;
import ru.sbt.edu_power.assist_bot.reporting.SlackReport;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.Element;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RegressionReport extends Element {
    private final String channelName;
    private final String userId;
    private final SlackReport slackReport;
    private static final String RELEASE_CHANNEL = "release_candidates-notify";

    public RegressionReport(final String channelName, final String userId) {
        this.channelName = channelName;
        this.userId = userId;
        slackReport = new SlackReport(
                SlackClient.getChannelId(RELEASE_CHANNEL),
                Duration.ofHours(3),
                null,
                "Публикация обобщённого отчёта в #release_candidates-notify",
                getLastReportTs(),
                ReportType.TOTAL_REPORT_TO_RELEASE,
                true
        );
    }

    public void renew() {
        slackReport.renew();
    }

    private String getLastReportTs() {
        final String channelId = SlackClient.getChannelId(channelName);
        final ReportSearch reportSearch = new ReportSearch(channelId, ReportType.TOTAL_REPORT_TO_RELEASE, userId);
        final List<Message> lastMessages = reportSearch.search();
        return lastMessages.isEmpty() ? null : lastMessages.get(0).getTs();
    }

    public void send(final RegressionReportCollector collector) {
        slackReport.updateReport(getReportBlocks(collector));
    }

    private List<LayoutBlock> getReportBlocks(final RegressionReportCollector collector) {
        final List<LayoutBlock> separatedBlock = new ArrayList<>();
        for (final RegressionData data : collector) {
            separatedBlock.addAll(getSingleReportBlocks(data));
            separatedBlock.add(DividerBlock.builder().build());
        }
        separatedBlock.remove(separatedBlock.size() - 1);
        return separatedBlock;
    }

    private List<LayoutBlock> getSingleReportBlocks(final RegressionData data) {
        final String testRunName = formatTestRunLink(data
                .getRegressAssistantTask()
                .getTestRunStorage()
                .getTestRunModel());
        final String progress = getTestRunProgressData(data);
        final List<String> issues = formatIssues(data);
        final List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(
                SectionBlock.builder().text(asMrkdn(String.format("%s\n%s", testRunName, progress))).build()
        );
        issues.stream()
              .map(i -> SectionBlock.builder().text(asMrkdn(i)).build())
              .forEach(blocks::add);
        return blocks;
    }

    private String getTestRunProgressData(final RegressionData data) {
        final int total = data.getAll();
        final int ended = data.getPassed();
        final int notEnded = total - ended;
        final int endedPercent = total == 0 ? 0 : ended * 100 / total;
        return String.format(
                "Всего: %d; Завершено: %d (%d%%); Осталось: %d",
                total,
                ended,
                endedPercent,
                notEnded
        );
    }

    private String formatTestRunLink(final TestRunModel testRunModel) {
        return String.format(
                "<%s%s|%s %s>",
                PropReader.get("jira.ui.testrun.endpoint"),
                testRunModel.getKey(),
                testRunModel.getKey(),
                testRunModel.getName()
        );
    }

    private List<String> formatIssues(final RegressionData data) {
        final IssueHandler issueHandler = new IssueHandler(
                data.getRegressAssistantTask().getTestRunStorage().getTestRunModel().getProject().name(),
                data.getRegressAssistantTask().getTestRunStorage().getTestRunModel().getJiraVersionModel().getId()
        );
        return issueHandler.formatIssues();
    }
}
