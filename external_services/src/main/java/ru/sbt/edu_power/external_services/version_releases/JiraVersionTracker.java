package ru.sbt.edu_power.external_services.version_releases;

import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс позволяет выполнять разбор версий джиры:
 *      получать список rc до необходимой
 *      получать список rc после необходимой
 *      получать список всех rc внутри заданной версии
 */
public class JiraVersionTracker {
    private final List<JiraVersionModel> versions;

    public JiraVersionTracker(final TCFields.ProjectId project) {
        versions = new JiraVersion().getNotReleasedVersions(project.id);
    }

    public List<JiraVersionModel> getVersionsBeforeRc(final JiraVersionModel version) {
        final ReleaseVersion releaseVersion = new ReleaseVersion(version.getName());
        if (releaseVersion.getRc() > 0) {
            final double rc = releaseVersion.getRc();
            return versions.stream()
                           .filter(v -> {
                               final ReleaseVersion rv = new ReleaseVersion(v.getName());
                               return releaseVersion.equalsByMainVersion(rv)
                                       && rv.getRc() < rc;
                           })
                           .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<JiraVersionModel> getVersionsAfterRc(final JiraVersionModel version) {
        final ReleaseVersion releaseVersion = new ReleaseVersion(version.getName());
        if (releaseVersion.getRc() > 0) {
            final double rc = releaseVersion.getRc();
            return versions.stream()
                           .filter(v -> {
                               final ReleaseVersion rv = new ReleaseVersion(v.getName());
                               return releaseVersion.equalsByMainVersion(rv)
                                      && rv.getRc() > rc;
                           })
                           .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<JiraVersionModel> getAllRc(final JiraVersionModel version) {
        final ReleaseVersion releaseVersion = new ReleaseVersion(version.getName());
        return versions.stream()
                       .filter(v -> releaseVersion.equalsByMainVersion(new ReleaseVersion(v.getName())))
                       .collect(Collectors.toList());
    }
}
