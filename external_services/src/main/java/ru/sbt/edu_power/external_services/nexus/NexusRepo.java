package ru.sbt.edu_power.external_services.nexus;

public enum NexusRepo {
    DB_DUMPS("db-dumps"),
    JAVA_E2E_STATS("java-e2e-stats");

    private String repoName;

    NexusRepo(final String repoName) {
        this.repoName = repoName;
    }

    public String getRepoName() {
        return repoName;
    }
}
