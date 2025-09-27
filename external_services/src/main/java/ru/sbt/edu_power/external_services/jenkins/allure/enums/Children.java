package ru.sbt.edu_power.external_services.jenkins.allure.enums;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public class Children {
    private String name;
    private String status;
    private String uid;
    private String parentUid;
    private AllureReport report;
    private String link;
    private Set<Children> children = new HashSet<>();

    public List<String> searchTags(final String projectKey) {
        Objects.requireNonNull(projectKey);
        final List<String> tags = getTags()
                .stream()
                .map(tag -> tag.replace("@", ""))
                .filter(t -> t.startsWith(projectKey))
                .collect(Collectors.toList());
        if (!tags.isEmpty()) {
            return tags;
        }
        // если в сценарии не сохранён тег по проекту, поищем его в названии сценария (актуально для JS тестов)
        final List<String> tagsFromName = Stream.of(name.split("\\s"))
                                                .filter(p -> p.startsWith("@" + projectKey))
                                                .map(t -> t.replace("@", ""))
                                                .collect(Collectors.toList());
        if (!tagsFromName.isEmpty()) {
            return tagsFromName;
        }
        return Collections.singletonList(name);
    }

    public List<String> getTags() {
        return report.getLabels()
                     .stream()
                     .filter(l -> "tag".equals(l.getName()))
                     .map(AllureReport.Label::getValue)
                     .collect(Collectors.toList());
    }
}
