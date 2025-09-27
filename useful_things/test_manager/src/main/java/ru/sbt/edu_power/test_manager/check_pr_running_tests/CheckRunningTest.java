package ru.sbt.edu_power.test_manager.check_pr_running_tests;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.bitbucket.BBConnection;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.pull_request.PullRequestModel;
import ru.sbt.edu_power.external_services.mattermost.Regressman;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class CheckRunningTest {
    final Pattern pattern = Pattern.compile(
            "https:\\/\\/jenkins3-dev\\.pcbltools\\.ru\\/job\\/EduPower\\/job\\/QA\\/job\\/(qa-team-[a-zA-Z0-9]+|qa-(java|js)-(ui(-auto|-parallel)?|api)?)");
    final Pattern patternPr = Pattern.compile("^(autotesting|end-2-end)");

    public void evaluate(final BBRepos repos) {

        final int day = getMaxDays();
        List<PullRequestModel> branchMerged = BBConnection
                .getPullRequest(repos)
                .stream()
                .filter(b -> !b.isOldEndCommit(day))
                .collect(
                        Collectors.toList());
        branchMerged.forEach(e -> {e.setActivities(BBConnection.getActivities(e.getId(), repos)); e.setPaths(BBConnection.getPaths(e.getId(), repos));});
        branchMerged = branchMerged.stream()
                                   .filter(pr -> Objects.nonNull(pr.getPaths()))
                                  .filter(p -> p.getPaths().stream()
                                                 .allMatch(path -> !patternPr.matcher(path.getPath().getParent()).find()))
                                   .collect(Collectors.toList());
        final List<PullRequestModel> branchesRunningTests = branchMerged
                .stream()
                .filter(b -> checkPrRunningTests(b.getActivities()))
                .collect(
                        Collectors.toList());
        branchMerged.forEach(pr -> {
            if (pattern.matcher(Objects.isNull(pr.getDescription()) ? "" : pr.getDescription()).find()) {
                branchesRunningTests.add(pr);
            }
        });
        List<PullRequestModel> branchesNoRunningTests = new ArrayList<>(branchMerged);
        for (PullRequestModel branch : branchesRunningTests) {
            branchesNoRunningTests.remove(branch);
        }
        final StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(String.format(
                "@channel\nВ репозиторий **\"%s\"**\nЗа %s дн влито в мастер %s веток \n",
                repos.getRepoName(),
                day,
                branchMerged.size()
        ));
        final String smail = branchesNoRunningTests.isEmpty() ? ":tada:" :":but:";
        stringBuffer.append(String.format("%s Влито %s веток без тестов%s :\n", smail,branchesNoRunningTests.size(),smail));
        if(!branchesNoRunningTests.isEmpty()){
            branchesNoRunningTests.forEach(e -> stringBuffer
                    .append(String.format("[%s](%s) - @%s\n", e.getId(), e.getLinkPR(), e.getSlackUserByPullRequest())));
        }
        Regressman.getInstance().sendPostByChannelName("pr-quality", stringBuffer.toString());

    }

    private int getMaxDays() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.MONDAY) {
            return 3;
        } else {
            return 1;
        }
    }

    private boolean checkPrRunningTests(List<PullRequestModel.Activities> activities) {
        for (PullRequestModel.Activities activity : activities) {
            if (Objects.nonNull(activity.getComment())) {
                if (checkCommentRunningTests(activity.getComment())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkCommentRunningTests(PullRequestModel.Comment comment) {
        if (comment.getText() != null) {
            if (!pattern.matcher(comment.getText()).find()) {
                if (!comment.getComments().isEmpty()) {
                    return comment
                            .getComments()
                            .stream()
                            .filter(e -> Objects.nonNull(e.getText()))
                            .anyMatch(com -> pattern.matcher(com.getText()).find());
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
        return false;
    }
}
