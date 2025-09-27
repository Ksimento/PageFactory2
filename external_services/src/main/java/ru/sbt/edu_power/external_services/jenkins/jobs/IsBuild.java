package ru.sbt.edu_power.external_services.jenkins.jobs;

import java.util.Map;

public interface IsBuild {
    void build();
    Map<String, Object> getParams();
    void setParams(Map<String, Object> params);
    String paramsToString();
    IsBuild addParam(String name, Object value);
}
