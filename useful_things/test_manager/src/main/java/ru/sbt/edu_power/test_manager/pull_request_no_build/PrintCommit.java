package ru.sbt.edu_power.test_manager.pull_request_no_build;


import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.bitbucket.models.pull_request.PullRequestModel;
import ru.sbt.edu_power.external_services.mattermost.Regressman;
import ru.sbt.edu_power.test_manager.Environment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
public class PrintCommit {
    public void print(List<PullRequestModel> branch) {
        if (branch.size() > 0){
            StringBuilder commitNoBranch = new StringBuilder();
            commitNoBranch.append("Список веток без билда:\n");
            branch.forEach(pr -> {
                        commitNoBranch.append(String.format("%s - %s - %s - %s\n",
                                pr.getLinkPR(),
                                pr.getAuthor().getUser().getName(),
                                pr.getLatestCommit().getUrl(),
                                new SimpleDateFormat("dd.MM.yyyy").format(new Date(pr.getClosedDate()))));
                    }
            );
        Regressman.getInstance().sendPostByChannelName(Environment.REPOSITORY.getChannel(), commitNoBranch.toString());
        } else {
            log.info("Веток без билда не обнаружено!");
        }
    }
}