package ru.sbt.edu_power.assist_bot.tasks.releases.open_branches;

import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.version_releases.ReleaseBranchHelper;
import ru.sbt.edu_power.external_services.version_releases.ReleaseVersion;
import ru.sbt.edu_power.external_services.version_releases.Release;
import ru.sbt.edu_power.external_services.version_releases.ReleaseProjectId;
import ru.sbt.edu_power.external_services.version_releases.Releases;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpenBranchesDispatcher implements IDispatcher {
    private final OpenBranchesView view;
    private BBRepos repo;
    private ReleaseProjectId releaseProjectId;
    private Releases releases;

    public OpenBranchesDispatcher(final OpenBranchesView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        repo = BBRepos.getByRepoName(view.getBbRepoSelectSection().getAccessory().getValue());
        releaseProjectId = determineReleaseProjectId();
        releases = new Releases();
        final ReleaseBranchHelper helper = new ReleaseBranchHelper(repo);
        final String message = String.format(
                "Открытые ветки в репозитории *%s*:\n%s",
                view.getBbRepoSelectSection().getAccessory().getValue(),
                formatBranches(helper.getAllOpenedMainBranches())
        );
        SlackClient.sendText(message, view.getUserId());
    }

    private String formatBranches(final Map<BranchModel, List<JiraUser>> data) {
        return data.entrySet()
                   .stream()
                   .sorted(Comparator.comparing(e -> new ReleaseVersion(e.getKey().getDisplayId())))
                   .map(e -> formatBranch(e.getKey(), e.getValue()))
                   .collect(Collectors.joining("\n"));
    }

    private String formatBranch(final BranchModel branch, final List<JiraUser> users) {
        final Optional<Release> releaseOptional = releases.searchRelease(releaseProjectId, branch.getDisplayId());
        final String branchName = branch.getDisplayId();
        final String userNames = users.isEmpty() ? "_открыто для всех_" :
                String.format(
                        "_открыто для пользователей (%s)_",
                        users.stream()
                             .map(JiraUser::getDisplayName)
                             .collect(Collectors.joining(", "))
                );
        final String installationDate;
        final String issue;
        final String icon;
        final String status;
        if (releaseOptional.isPresent()) {
            final boolean isReleased = releaseOptional.get().isReleased();
            final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            final Date date = isReleased ?
                    releaseOptional.get().getDateTime(IssueFields.Field.RESOLVED) :
                    releaseOptional.get().getDateTime(IssueFields.Field.INSTALL_PLANE_DATE);
            final String dateType = isReleased ?
                    "Релиз " : "План ";
            // показываем дату фичефриза в случае, если она установлена, если это минор или хотфикс
            final boolean isFfVisible = Objects.nonNull(
                    releaseOptional.get().getIssueFields().get(IssueFields.Field.FEATURE_FREEZE))
                                        && !releaseOptional.get().getReleaseVersion().withoutVersion()
                                        && (releaseOptional.get().getReleaseVersion().getMinor() > 0
                                            || releaseOptional.get().getReleaseVersion().getHotfix() > 0
                                        );
            if (isFfVisible) {
                installationDate = String.format(
                        "%s%s / ФФ %s",
                        dateType,
                        Objects.nonNull(date) ? formatter.format(date) : "Дата не установлена",
                        formatter.format(releaseOptional.get().getDateTime(IssueFields.Field.FEATURE_FREEZE))
                );
            } else {
                installationDate = dateType + (Objects.nonNull(date) ? formatter.format(date) : "Дата не установлена");
            }
            issue = releaseOptional.get().getJiraIssueLink();
            icon = isReleased ? ":white_check_mark:" : ":wheelchair:";
            status = releaseOptional.get().getStatus();
        } else {
            installationDate = "Дата установки не определена";
            issue = "Задача не найдена";
            icon = ":zzz:";
            status = "Статус неизвестен";
        }
        return String.format(
                "*%s*: %s / %s / %s / %s %s",
                branchName,
                userNames,
                installationDate,
                issue,
                status,
                icon
        );
    }

    private ReleaseProjectId determineReleaseProjectId() {
        switch (repo) {
            case EDU_FRONT:
            case EDU_BACK:
                return ReleaseProjectId.SBERCLASS;
            case S21_APPLICATION_EXAM:
            case S21_APPLICATION:
                return ReleaseProjectId.S21;
            case MFE_DEPLOY_VERSION:
                return ReleaseProjectId.MFE;
            default:
                throw new AssistBotException("Нет обработчика для типа " + repo.name());
        }
    }
}
