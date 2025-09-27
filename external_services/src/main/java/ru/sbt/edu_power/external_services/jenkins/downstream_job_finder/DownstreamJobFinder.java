package ru.sbt.edu_power.external_services.jenkins.downstream_job_finder;

import ru.sbt.edu_power.external_services.jenkins.JenkinsHttpConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DownstreamJobFinder {
    private final String downstreamJobUrl;
    private final List<BuildElement> buildElementList = new ArrayList<>();

    public DownstreamJobFinder(final String downstreamJobUrl) {
        this.downstreamJobUrl = downstreamJobUrl;
    }

    public List<BuildElement> search(final Integer upstreamJobId) {
        return getBuildElementList()
                .stream()
                .filter(b -> getUpstreamBuild(b.getActions()).contains(upstreamJobId))
                .collect(Collectors.toList());
    }

    public List<BuildElement> getBuildElementList() {
        if (buildElementList.isEmpty()) {
            collectBuildElements();
        }
        return buildElementList;
    }

    private void collectBuildElements() {
        buildElementList.addAll(JenkinsHttpConnection.getClusterByUrl(downstreamJobUrl).getAllBuilds(downstreamJobUrl));
    }

    public List<Integer> getUpstreamBuild(final Set<BuildActions> actions) {
        return actions
                .stream()
                .filter(a -> !a.getCauses().isEmpty())
                .filter(a -> a.getCauses().stream().noneMatch(m -> m.containsValue("com.sonyericsson.rebuild.RebuildCause")))
                .flatMap(a -> a.getCauses().stream())
                .filter(m -> m.containsKey("upstreamBuild"))
                .map(m -> (Double) m.get("upstreamBuild"))
                .map(Double::intValue)
                .collect(Collectors.toList());
    }

    public String getParamByName(final Set<BuildActions> actions, final String paramName) {
        return actions.stream()
                      .filter(a -> !a.getParameters().isEmpty())
                      .flatMap(a -> a.getParameters().stream())
                      .filter(m -> paramName.equals(m.get("name")))
                      .map(m -> m.get("value").toString())
                      .findFirst()
                      .orElse("");
    }
}
