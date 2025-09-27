package ru.sbt.edu_power.test_manager.old_branch_notify;

import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;

import java.util.stream.Stream;

public enum BBReposToChannelEnum {
    EDU_BACK(BBRepos.EDU_BACK, "backend"),
    EDU_FRONT(BBRepos.EDU_FRONT, "frontend"),
    S21_APPLICATION(BBRepos.S21_APPLICATION, "team_school_21"),
    S21_APPLICATION_EXAM(BBRepos.S21_APPLICATION_EXAM, "team_school_21"),
    MFE_DEPLOY_VERSION(BBRepos.MFE_DEPLOY_VERSION, "go-to-mfe"),
    ;

    private final BBRepos repo;
    private final String channel;

    BBReposToChannelEnum(final BBRepos repo, final String channel) {
        this.repo = repo;
        this.channel = channel;
    }

    public BBRepos getRepo() {
        return repo;
    }

    public String getChannel() {
        return channel;
    }

    public static BBReposToChannelEnum determine(final String repo) {
        return Stream.of(BBReposToChannelEnum.values())
                .filter(v -> v.name().equals(repo))
                .findFirst()
                .orElse(null);
    }
}
