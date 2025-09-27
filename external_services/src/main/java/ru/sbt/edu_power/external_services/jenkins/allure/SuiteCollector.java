package ru.sbt.edu_power.external_services.jenkins.allure;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Teams;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class SuiteCollector {
    @Getter
    private final Map<String, List<Children>> collection = new HashMap<>();
    private final Gson gson = new Gson();

    public void recoverCollector(final String json) {
        final Type typeOfT = new TypeToken<Map<String, List<Children>>>() {
        }.getType();
        collection.putAll(gson.fromJson(json, typeOfT));
    }

    public int getTestCount() {
        return collection.values()
                .stream()
                .mapToInt(List::size)
                .reduce(Integer::sum)
                .orElse(0);
    }

    public void add(final AllureHelper allure) {
        final List<Children> children = allure.getChildrenList();
        children.forEach(ch -> {
            final String javaTag = ch.searchTags("")
                    .stream()
                    .filter(e -> Arrays.stream(Teams.values())
                            .map(Teams::name)
                            .collect(Collectors.toList())
                            .contains(e))
                    .findFirst()
                    .orElse("");
            if (!javaTag.isEmpty()) {
                addByTeam(javaTag, ch);
                return;
            }
            addByTeam("Untagged", ch);
        });
    }

    private void addByTeam(final String team, final Children children) {
        if (!collection.containsKey(team)) {
            collection.put(team, new ArrayList<>());
        }
        collection.get(team).add(children);
    }
}
