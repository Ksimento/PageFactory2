package ru.sbt.edu_power.external_services.bitbucket.models.pull_request;

import lombok.Getter;
import lombok.Setter;
import net.bis5.mattermost.model.User;
import ru.sbt.edu_power.external_services.mattermost.MattermostUsers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Getter
@Setter
public class PullRequestModel {
    private String id;
    private String title;
    private String description;
    private String state;
    private long closedDate;
    private LatestCommitPullRequest latestCommit;
    private String linkPR;
    private FromRef fromRef;
    private ToRef toRef;
    private Author author;
    private List<Activities> activities = null;
    private List<Paths> paths = null;

    @Getter
    @Setter
    public static class FromRef {
        private String id;
        private String displayId;
        private String latestCommit;
    }

    @Getter
    @Setter
    public static class ToRef{
        private String id;
        private String displayId;
    }

    @Getter
    @Setter
    public static class Author{
        private User user;
    }

    @Getter
    @Setter
    public static class User{
        private String name;
        private String emailAddress;
        private String displayName;
    }

    @Getter
    @Setter
    public static class Reviewers{
        private List<User> reviewers;
    }

    @Getter
    @Setter
    public static class Activities {
        private long id;
        private long createdDate;
        private boolean action;
        private AuthorActivities author;
        private String commentAction;
        private Comment comment;

    }
    @Getter
    @Setter
    public static class Comment {
        private String text;
        private AuthorActivities author;
        private long createdDate;
        private long updatedDate;
        private List<Comments> comments;
    }
    @Getter
    @Setter
    public static class Comments {
        private String text;
        private AuthorActivities author;
        private long createdDate;
        private long updatedDate;
    }
    @Getter
    @Setter
    public static class AuthorActivities {
        private String name;
        private String emailAddress;
        private String displayName;
        private boolean active;
    }

    @Getter
    @Setter
    public static class Paths {
        private Path path;
    }
    @Getter
    @Setter
    public static class Path {
        private String parent;
    }
    public boolean isOldEndCommit(final int maxDays){
        final LocalDateTime commitDate = LocalDateTime.ofEpochSecond(
                getClosedDate() / 1000,
                0,
                ZoneOffset.UTC
        );
        return Duration.between(commitDate, LocalDateTime.now()).toDays() > maxDays;
    }

    public String getSlackUserByPullRequest() {
        return MattermostUsers.getInstance().getUserByEmail(
                author.getUser().emailAddress
        ).getUsername();
    }
}