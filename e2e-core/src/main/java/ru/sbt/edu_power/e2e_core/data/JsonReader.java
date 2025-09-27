package ru.sbt.edu_power.e2e_core.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.sbt.edu_power.external_services.data.Archiver;
import ru.sbt.edu_power.external_services.validator.Validator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JsonReader {
    private final File resources;

    public JsonReader(final String fileName) {
        if (fileName.endsWith("zip")) {
            final Path path = Paths.get(fileName.replace(".zip", ""), "unzipped.json");
            final Archiver archiver = new Archiver().setArchivedFile(fileName);
            archiver.unZipArchive("*json").writeSource(path);
            this.resources = path.toFile();
        } else {
            this.resources = new File(fileName);
        }
    }

    public boolean verifyValueByPath(final List<String> path) {
        final String value = path.get(path.size() - 1);
        path.remove(path.size() - 1);
        return initiateReader((map) -> Validator.matchValues(stepInto(map, path), value));
    }

    public boolean verifyValueByKey(final String key, final String value) {
        return initiateReader((map) -> searchPair(map, key, value));
    }


    private boolean searchPair(final Map<String, Object> map, final String key, final String value) {
        final Object item = map.get(key);
        if (item instanceof String || item instanceof Number || item instanceof Boolean) {
            final String actual;
            if (item instanceof Number) {
                actual = String.valueOf(item).replace(".0", "");
            } else {
                actual = String.valueOf(item);
            }
            if (Validator.matchValues(actual, value)) {
                return true;
            }
        }
        return map.keySet()
                  .stream()
                  .filter(mapKey -> map.get(mapKey) instanceof Map)
                  .anyMatch(mapKey -> searchPair((Map) map.get(mapKey), key, value));
    }

    private String stepInto(final Map<String, Object> map, final List<String> path) {
        final String key = path.remove(0);
        final Object item = map.get(key);
        if (item instanceof String) {
            if (!path.isEmpty()) {
                throw new DataProcessingException("JSON закончился раньше, чем путь до значения: " + path);
            }
            return (String) item;
        }
        if (!path.isEmpty()) {
            if (item instanceof List) {
                final int listKey = Integer.parseInt(path.remove(0));
                final Object listItem = ((List<Object>) item).get(listKey);
                if (listItem instanceof Map) {
                    stepInto((Map<String, Object>) listItem, path);
                } else if (listItem instanceof String) {
                    return (String) listItem;
                }
            } else if (item instanceof Map) {
                return stepInto((Map<String, Object>) map.get(key), path);
            }
        }
        return null;
    }

    private <T> T initiateReader(final Function<Map<String, Object>, T> function) {
        final Gson gson = new Gson();
        try (final FileReader reader = new FileReader(resources)) {
            final Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            final Map<String, Object> map = gson.fromJson(reader, type);
            return function.apply(map);
        } catch (final IOException e) {
            throw new DataProcessingException(e);
        }
    }
}
