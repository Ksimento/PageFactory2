package ru.sbt.edu_power.external_services.jenkins.downstream_job_finder;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class BuildElement {
    private Integer number;
    private String result;
    private Long timestamp;
    private Set<BuildActions> actions = new HashSet<>();
    private String url;
    private String slackIcon;
    private String displayName;
    private Integer queueId;
    private Long duration;
}
