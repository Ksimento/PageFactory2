package ru.sbt.edu_power.external_services.version_releases;

import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MasterVersion {
    private final Releases releases = new Releases();

    public String getMasterVersion(final ReleaseProjectId releaseProjectId) {
        final BBRepos repo = getRepo(releaseProjectId);
        if (Objects.isNull(repo)) {
            return "Поддержка этого проекта не реализована, обратитесь к тимлиду релизной команды";
        }
        final ReleaseBranchHelper releaseBranchHelper = new ReleaseBranchHelper(repo);
        final List<String> openBranches = releaseBranchHelper.getAllOpenedMainBranches()
                .keySet()
                .stream()
                .map(BranchModel::getDisplayId)
                .collect(Collectors.toList());

        return releases.getReleases()
                .stream()
                .filter(r -> r.getReleaseProjectId() == releaseProjectId)
                .filter(r -> r.getStatus().equals(IssueStatus.OPEN.getValue()))
                .map(Release::getReleaseVersion)
                .filter(v -> v.getVersionType() != VersionType.UNDEFINED)
                .filter(v -> v.getMinor() == 0)
                .sorted()
                .map(ReleaseVersion::toString)
                .filter(s -> !openBranches.contains(s))
                .findFirst()
                .orElse("Открытых релизных задач нет");
    }

    private BBRepos getRepo(final ReleaseProjectId releaseProjectId) {
        switch (releaseProjectId) {
            case SBERCLASS:
                return BBRepos.EDU_BACK;
            case S21:
                return BBRepos.S21_APPLICATION;
            case MFE:
                return BBRepos.MFE_DEPLOY_VERSION;
            default:
                return null;
        }
    }
}
