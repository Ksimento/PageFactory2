package ru.sbt.edu_power.external_services.bitbucket.models;

import ru.sbt.edu_power.external_services.ExternalServicesException;

import java.util.stream.Stream;

// названия репозиториев
public enum BBRepos {
    EDU_FRONT("edu-front", BBProjects.EDUPOWER),
    EDU_BACK("edu-back", BBProjects.EDUPOWER),
    S21_APPLICATION("s21-application", BBProjects.EDUPOWER),
    S21_APPLICATION_EXAM("s21-application-exam", BBProjects.EDUPOWER),
    S21_E2E_JAVA_APP("s21-e2e-java-app", BBProjects.EDUPOWER),
    MFE_DEPLOY_VERSION("mfe-deploy-version", BBProjects.FRS),

    PROJECT_S21_APPLICATION("s21-application", BBProjects.S21),

    PROJECT_S21_APPLICATION_EXAM("s21-application-exam", BBProjects.S21),

    PROJECT_S21_ADMINISTRATION("s21-administration", BBProjects.S21),
    ;

    private final String repoName;
    private final BBProjects project;

    BBRepos(final String repoName, final BBProjects project) {
        this.repoName = repoName;
        this.project = project;
    }

    public String getRepoName() {
        return repoName;
    }

    public BBProjects getProject() {
        return project;
    }

    public static BBRepos getByRepoName(final String repoName) {
        return Stream.of(values())
                     .filter(rn -> repoName.equals(rn.repoName))
                     .findFirst()
                     .orElseThrow(() -> new ExternalServicesException("Не найдено репозитория для значения " +
                                                                      repoName));
    }
}
