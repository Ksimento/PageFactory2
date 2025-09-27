package ru.sbt.edu_power.external_services.jira.agile.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class JiraVersionModel implements Comparable<JiraVersionModel>, Serializable {
    private static final long serialVersionUID = -6826977490877333583L;
    private String name;
    private boolean archived;
    private boolean released;
    private Integer projectId;
    private String id;


    @Override
    public int compareTo(final JiraVersionModel o) {
        return name.compareTo(o.name);
    }
}
