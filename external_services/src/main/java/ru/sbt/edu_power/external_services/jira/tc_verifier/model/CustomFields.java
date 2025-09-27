package ru.sbt.edu_power.external_services.jira.tc_verifier.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
public class CustomFields {
    private Boolean archived;
    private Integer id;
    private String name;
    private Integer projectId;
    private Set<CustomFieldOptions> options = new HashSet<>();

    public String getOptionById(final Integer optionId) {
        return options.stream()
                .filter(o -> !o.archived)
                .filter(o -> o.id.equals(optionId))
                .map(CustomFieldOptions::getName)
                .findFirst()
                .orElseThrow(() -> new JiraConnectionException("Не найдена опция по ID " + optionId));
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class CustomFieldOptions {
        private Boolean archived;
        private String name;
        private Integer id;
    }
}
