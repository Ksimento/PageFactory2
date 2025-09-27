package ru.sbt.edu_power.external_services.jenkins.allure.enums;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class Suite {
    private String name;
    private String uid;
    private Set<Children> children = new HashSet<>();

    public Children getChildrenByName(final String name) {
        return children.stream()
                       .filter(c -> name.equals(c.getName()))
                       .findFirst()
                       .orElse(null);
    }
}
