package ru.sbt.edu_power.external_services.jenkins.downstream_job_finder;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BuildActions {
    private String _class;
    private List<Map<String, Object>> causes = new ArrayList<>();
    private List<Map<String, Object>> parameters = new ArrayList<>();
}
