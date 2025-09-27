package ru.sbt.edu_power.external_services.jira.agile.model;

import com.google.common.base.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Модель профиля пользователя в джире
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JiraUser {
    private String name;
    private String emailAddress;
    private String displayName;
    private String timeZone;
    private boolean isFakeUser;
    private boolean active;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JiraUser jiraUser = (JiraUser) o;
        return Objects.equal(name, jiraUser.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
