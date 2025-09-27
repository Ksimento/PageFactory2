package ru.sbt.edu_power.assist_bot.yaml_configurations;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import ru.sbt.edu_power.assist_bot.AssistBotException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class YamlConfigurator {
    private final String yamlSource;
    private Object model;
    private Yaml yaml;

    public YamlConfigurator(final String yamlSource) {
        this.yamlSource = yamlSource;
    }

    @SuppressWarnings("unchecked")
    public <T> T load(final Class<T> type) {
        yaml = new Yaml(new Constructor(type));
        try (final InputStream is = Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(yamlSource))) {
            model = yaml.load(is);
        } catch (final IOException e) {
            throw new AssistBotException("Не удалось загрузить ресурс " + yamlSource, e);
        }
        return (T) model;
    }

    public String toString() {
        return Objects.requireNonNull(yaml).dumpAsMap(model);
    }
}
