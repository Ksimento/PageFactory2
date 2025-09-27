package ru.sbt.edu_power.e2e_core.devtools;

import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import cucumber.api.Scenario;

import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RuntimeConsoleErrorTracking {
    private final Runtime runtime;
    private final Map<String, String> errors = new ConcurrentHashMap<>();
    private final ErrorExclusion errorExclusion;

    public RuntimeConsoleErrorTracking(final Runtime runtime) {
        this.runtime = runtime;
        errorExclusion = new ErrorExclusion();
    }

    public void enable() {
        runtime.onConsoleAPICalled(event -> {
            if ("ERROR".equals(event.getType().name())) {
                final String message = event
                        .getArgs()
                        .stream()
                        .map(RemoteObject::getDescription)
                        .filter(m -> Objects.nonNull(m) && !"Object".equals(m))
                        .collect(Collectors.joining("\n\n"));
                if (!"".equals(message) && errorExclusion.isErrorNotAllowed(message)) {
                    final String time = new Date(event.getTimestamp().longValue())
                            .toInstant()
                            .atZone(ZoneId.of("Europe/Moscow"))
                            .toLocalDateTime()
                            .toString();
                    errors.put(time, message);
                }
            }
        });
    }

    public void initErrorExclusion(final Scenario scenario) {
        errorExclusion.init(scenario);
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void clearErrors() {
        errors.clear();
    }
}
