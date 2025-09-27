package ru.sbt.edu_power.external_services.bitbucket.models;

import lombok.Getter;
import lombok.Setter;
import ru.sbt.edu_power.external_services.ExternalServicesException;

import java.util.Set;
import java.util.stream.Stream;

@Getter
@Setter
public class BranchRestrictionModel {
    private String type;
    private Matcher matcher;
    private Set<AccessUser> users;

    public RestrictionType getType() {
        return RestrictionType.getByType(type);
    }

    public boolean isBranch() {
        return "BRANCH".equals(matcher.type.id);
    }

    public boolean isPattern() {
        return "PATTERN".equals(matcher.type.id);
    }

    public String getBranchName() {
        return matcher.displayId;
    }

    @Getter
    @Setter
    public static class Matcher {
        private String id;
        private String displayId;
        private boolean active;
        private MatcherType type;

        @Getter
        @Setter
        public static class MatcherType {
            private String id;
        }
    }

    @Getter
    @Setter
    public static class AccessUser {
        private String name;
        private String emailAddress;
        private String displayName;
        private String active;
    }

    public enum RestrictionType {
        FAST_FORWARD_ONLY("fast-forward-only"),
        NO_DELETES("no-deletes"),
        PULL_REQUEST_ONLY("pull-request-only"),
        READ_ONLY("read-only");

        private final String type;

        RestrictionType(final String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static RestrictionType getByType(final String typeName) {
            return Stream.of(RestrictionType.values())
                         .filter(v -> v.type.equals(typeName))
                         .findFirst()
                         .orElseThrow(() -> new ExternalServicesException("Не найдено значения для типа " + typeName));
        }
    }
}
