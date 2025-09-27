package ru.sbt.edu_power.external_services.version_releases;

import lombok.Getter;
import lombok.SneakyThrows;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

// Релизная задача из джиры
@Getter
public class Release implements Comparable<Release> {
    private final String key;
    private final IssueFields issueFields;
    private final ReleaseProjectId releaseProjectId;
    private final ReleaseVersion releaseVersion;
    private static final String JIRA_ISSUE_LINK = PropReader.get("jira.issue.link");

    public Release(final String key, final IssueFields issueFields) {
        this.key = key;
        this.issueFields = issueFields;
        releaseProjectId = getProjectId();
        final String fixVersions = issueFields.get(IssueFields.Field.FIX_VERSIONS);
        if (Objects.nonNull(fixVersions)) {
            releaseVersion = new ReleaseVersion(fixVersions);
        } else {
            final String title = issueFields.get(IssueFields.Field.SUMMARY);
            if (Objects.nonNull(title)) {
                final String[] parts = title.split("\\s");
                releaseVersion = new ReleaseVersion(parts[parts.length - 1]);
            } else {
                releaseVersion = new ReleaseVersion(null);
            }
        }
    }

    public String toString(final ZoneId userZoneId) {
        final String icon = isReleased() ? ":white_check_mark:" : ":wheelchair:";
        final String date;
        if (isReleased()) {
            date = "Установлен " + formatDate(getReleaseDate(), userZoneId);
        } else {
            final String featureFreezeDate = getFeatureFreezeDate();
            if (Objects.isNull(getFeatureFreezeDate())) {
                date = "Дата не определена";
            } else {
                final String planDate = (Objects.isNull(getPlanDate())) ? "" :
                        "/ План " + formatDate(getPlanDate(), userZoneId);
                date = "ФФ " + formatDate(featureFreezeDate, userZoneId) + planDate;
            }
        }
        final String version;
        if (releaseProjectId == ReleaseProjectId.UNDEFINED) {
            version = issueFields.get(IssueFields.Field.SUMMARY);
        } else {
            version = releaseVersion.formatWithoutRc();
        }
        return String.format(
                "%s *%s*: _%s_ / %s / %s",
                icon,
                version,
                getStatus(),
                date,
                getJiraIssueLink()
        );
    }

    // получить форматированную для слака ссылку на задачу в джире
    public String getJiraIssueLink() {
        return String.format("<%s|%s>", JIRA_ISSUE_LINK.replace("{ISSUE_KEY}", key), key);
    }

    public String getFeatureFreezeDate() {
        return issueFields.get(IssueFields.Field.FEATURE_FREEZE);
    }

    public String getPlanDate() {
        return issueFields.get(IssueFields.Field.INSTALL_PLANE_DATE);
    }

    public boolean isReleased() {
        return IssueStatus.RELEASE_INSTALLED.getValue().equals(issueFields.get(IssueFields.Field.STATUS));
    }

    public String getReleaseDate() {
        return issueFields.get(IssueFields.Field.RESOLVED);
    }

    public String getStatus() {
        return issueFields.get(IssueFields.Field.STATUS);
    }

    public String getProjectName() {
        return releaseProjectId.getService();
    }

    @SneakyThrows
    public Date getDateTime(final IssueFields.Field dateField) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'H:m:s.SSSZ");
        final String date = issueFields.get(dateField);
        if (Objects.isNull(date)) {
            return null;
        }
        return formatter.parse(date);
    }

    @SneakyThrows
    private String formatDate(final String dateString, final ZoneId zoneId) {
        if (Objects.isNull(dateString) || dateString.isEmpty()) {
            return "дата не определена";
        }
        final SimpleDateFormat parseDate = new SimpleDateFormat("yyyy-MM-dd'T'H:m:s.SSSZ");
        final Date date = parseDate.parse(dateString);
        final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        format.setTimeZone(TimeZone.getTimeZone(zoneId));
        return format.format(date);
    }

    private ReleaseProjectId getProjectId() {
        final String releaseObject = issueFields.get(IssueFields.Field.RELEASE_OBJECT);
        final ReleaseProjectId releaseProjectId = ReleaseProjectId.getByService(releaseObject);
        final String issueTitle = issueFields.get(IssueFields.Field.SUMMARY);
        if (Objects.nonNull(issueTitle)) {
            if (issueTitle.contains("hasura")) {
                return ReleaseProjectId.HASURA;
            }
            if (releaseProjectId == ReleaseProjectId.SBERCLASS) {
                if (issueTitle.contains("MFE")) {
                    return ReleaseProjectId.MFE;
                } else if (issueTitle.contains("пользоват")) {
                    return ReleaseProjectId.USER_SERVICE;
                } else if (issueTitle.contains("edu-schedule-service")) {
                    return ReleaseProjectId.EDU_SCHEDULE_SERVICE;
                } else if (issueTitle.contains("каталога")) {
                    return ReleaseProjectId.CATALOG;
                } else {
                    return ReleaseProjectId.SBERCLASS;
                }
            }
        }
        return releaseProjectId;
    }

    @Override
    public int compareTo(final Release o) {
        if (Objects.isNull(releaseVersion)) {
            return -1;
        }
        if (Objects.isNull(o.releaseVersion)) {
            return 1;
        }
        return releaseVersion.compareTo(o.releaseVersion);
    }
}
