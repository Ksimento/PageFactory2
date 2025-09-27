package ru.sbt.edu_power.assist_bot.tasks.releases.release_status;

import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.Element;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.tasks.releases.regression_report.IssueHandler;

import java.util.List;
import java.util.stream.Collectors;

public class ReleaseStatusDispatcher extends Element implements IDispatcher {

    private final ReleaseStatusView view;

    public ReleaseStatusDispatcher(final ReleaseStatusView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        final IssueHandler issueHandler = new IssueHandler(
                view.getJiraProjectSelectSection()
                    .getAccessory()
                    .getValue(),
                view.getJiraVersionSelectSection()
                    .getAccessory()
                    .getValue()
        );
        final List<LayoutBlock> blocks = issueHandler.formatIssues()
                           .stream()
                           .map(m -> SectionBlock.builder().text(asMrkdn(m)).build())
                           .collect(Collectors.toList());
        SlackClient.sendBlock(
                blocks,
                "Статус релиза",
                view.getUserId()
        );
    }
}
