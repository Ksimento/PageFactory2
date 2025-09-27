package ru.sbt.edu_power.external_services.jira;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;

public class JiraExporterUtils {
    
    public static String getJson(final Object object) {
        final Gson gson = new Gson();
        return gson.toJson(object);
    }

    public static String decodeData(final String data) {
        return new String(
                Base64.getDecoder().decode(data),
                StandardCharsets.UTF_8
        ).trim();
    }

    public static String getAbsolutePath(final String path) {
        return Paths.get(path).toAbsolutePath().toString();
    }
}
