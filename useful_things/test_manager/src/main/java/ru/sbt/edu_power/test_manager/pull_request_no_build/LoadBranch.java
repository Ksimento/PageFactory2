package ru.sbt.edu_power.test_manager.pull_request_no_build;

import lombok.extern.slf4j.Slf4j;

import ru.sbt.edu_power.external_services.bitbucket.BBConnection;
import ru.sbt.edu_power.external_services.bitbucket.models.pull_request.PullRequestModel;
import ru.sbt.edu_power.test_manager.Environment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class LoadBranch {

    public List<PullRequestModel> evaluate() {
        return BBConnection.getPullRequest(Environment.REPOSITORY.getRepo())
                .stream()
                .filter(pr ->
                        dateFilter(pr.getClosedDate())
                )
                .map(pr -> {
                    String commit = pr.getFromRef().getLatestCommit();
                    pr.setLatestCommit(BBConnection.getLatestCommitPullRequest(commit));
                    return pr;
                })
                .collect(Collectors.toList());
    }

    private boolean dateFilter(long closeDate) {
        String dateMinusWeek = LocalDateTime.now().minusWeeks(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        Date date = new Date();
        try {
            date = new SimpleDateFormat("dd.MM.yyyy").parse(dateMinusWeek);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return closeDate >= date.getTime();
    }
}