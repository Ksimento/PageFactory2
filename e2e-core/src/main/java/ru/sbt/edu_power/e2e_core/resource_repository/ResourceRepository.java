package ru.sbt.edu_power.e2e_core.resource_repository;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class ResourceRepository {

    private static final String JSON_PATH = Props.get("user.profile.set");

    public enum AvailableResource {
        USERS("users.resources"),
        SVG_ICONS("svg.icons.resources"),
        URLS("url.map.resources"),
        CORRECT_ERROR_MESSAGE("correct.error.message.resources"),
        USER_AGENT("browser.user.agent.resources"),
        REQUEST_MAP("request.map.resources"),
        HTML_SOURCE("html.source.resources"),
        SMOKE_LAYOUT_USERS("smoke.layout.auth.data");
        private final String name;

        AvailableResource(final String name) {
            this.name = name;
        }

        String getName() {
            return this.name;
        }
    }

    private ResourceRepository() {
    }

    private static final EnumMap<AvailableResource, Map<String, ?>> RESOURCES = new EnumMap<>(AvailableResource.class);

    public static synchronized <T> T getResource(final AvailableResource availableResource) {
        if (!RESOURCES.containsKey(availableResource)) {
            collect();
        }
        return (T) RESOURCES.get(availableResource);
    }

    public static List<String> getResourceAsList(final AvailableResource availableResource) {
        final Map<String, String> map = getResource(availableResource);
        return new ArrayList<>(map.values());
    }

    public static Map<String, String> getResourceAsMap(final AvailableResource availableResource) {
        return new HashMap<>(getResource(availableResource));
    }

    private static void collect() {
        final ResourceRepository parser = new ResourceRepository();
        for (final AvailableResource availableResource : AvailableResource.values()) {
            parser.collect(availableResource);
        }
    }

    private void collect(final AvailableResource availableResource) {
        final Path path = Paths.get(System.getProperty(availableResource.getName()));
        if (path.toString().endsWith(".json")) {
            jsonParser(path, availableResource);
        } else if (path.toString().endsWith(".resources")) {
            resParser(path, availableResource);
        } else {
            throw new AutotestError("Неизвестный формат файла " + path.getFileName());
        }
    }

    private void jsonParser(final Path path, final AvailableResource resName) {
        try {
            final Gson gson = new Gson();
            final Type type = new TypeToken<Map<String, Map<String, Map<String, String>>>>(){}.getType();
            final Map<String, Map<String, Map<String, String>>> map =
                    gson.fromJson(FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8), type);
            RESOURCES.put(resName, map);
        } catch (final IOException e) {
            throw new AutotestError("Файл не доступен " + path, e);
        }
    }

    private void resParser(final Path path, final AvailableResource resName) {
        final Map<String, String> resourcesMap = new HashMap<>();
        try {
            Files.readAllLines(path).stream()
                 .filter(line -> !line.startsWith("#") && line.contains("="))
                 .forEach(line -> {
                     final String name = line.split("=", 2)[0].trim();
                     final String value = line.split("=", 2)[1].trim();
                     resourcesMap.put(name, value);
                 });
        } catch (final IOException e) {
            throw new AutotestError("Файл не доступен " + path, e);
        }
        RESOURCES.put(resName, resourcesMap);
    }
}
