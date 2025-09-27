package ru.sbt.edu_power.assist_bot.tasks.full_deploy;

import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.composition.PlainTextObject;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.assist_bot.services.jenkins.DockerTagSearch;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FullDeployUtils {
    public static List<Option> getTagSuggestionOptions(final DockerTagSearch.Repo repo, final String keyword) {
        if ("develop".contains(keyword)) {
            return Collections.singletonList(
                    Option.builder()
                          .text(PlainTextObject.builder().text("develop").emoji(false).build())
                          .value("develop")
                          .build()
            );
        }
        return getTagMap(repo, keyword, true).entrySet()
                                             .stream()
                                             .map(e -> Option
                                                     .builder()
                                                     .text(PlainTextObject
                                                             .builder()
                                                             .text(e.getKey())
                                                             .emoji(true)
                                                             .build())
                                                     .value(e.getValue())
                                                     .build())
                                             .collect(Collectors.toList());
    }

    public static List<OptionObject> getTagOptions(
            final DockerTagSearch.Repo repo,
            final String keyword,
            final boolean limitFieldLength
    ) {
        return getTagMap(repo, keyword, limitFieldLength).entrySet()
                                                         .stream()
                                                         .map(e -> OptionObject
                                                                 .builder()
                                                                 .text(PlainTextObject
                                                                         .builder()
                                                                         .text(e.getKey())
                                                                         .emoji(true)
                                                                         .build())
                                                                 .value(e.getValue())
                                                                 .build())
                                                         .filter(ESUtils.distinctByKey(OptionObject::getValue))
                                                         .collect(Collectors.toList());
    }

    public static Map<String, String> getTagMap(
            final DockerTagSearch.Repo repo,
            final String keyword,
            final boolean limitFieldLength
    ) {
        return DockerTagSearch.getInstance().getDockerTag(repo, keyword)
                              .entrySet()
                              .stream()
                              .sorted((a, b) -> b.getKey().getTimestamp().compareTo(a.getKey().getTimestamp()))
                              .limit(8)
                              .collect(Collectors.toMap(
                                      e -> constructOptionText(e.getKey(), e.getValue(), limitFieldLength),
                                      e -> e.getKey().getDisplayName(),
                                      (a, b) -> b, LinkedHashMap::new
                              ));
    }

    private static String constructOptionText(
            final BuildElement element,
            final DockerTagSearch.Cluster cluster,
            final boolean limitFieldLength
    ) {
        final String clusterName = "(" + cluster.name() + ")";
        final String emoji = element.getSlackIcon();
        final String time = formatDuration(Duration.ofMillis(System.currentTimeMillis() - element.getTimestamp()));
        final String optionText = String.join(" ", emoji, clusterName, time, element.getDisplayName());
        return limitFieldLength ? optionText.substring(0, Math.min(optionText.length(), 60)) : optionText;
    }

    private static String formatDuration(final Duration duration) {
        if (duration.toMinutes() < 60L) {
            return duration.toMinutes() + "m.";
        }
        if (duration.toHours() < 24L) {
            return duration.toHours() + "h.";
        }
        return duration.toDays() + "d.";
    }
}
