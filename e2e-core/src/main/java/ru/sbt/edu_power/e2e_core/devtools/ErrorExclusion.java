package ru.sbt.edu_power.e2e_core.devtools;

import cucumber.api.Scenario;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Класс хранит специфичные исключения для фронт/бэк ошибок для каждого из сценариев
public class ErrorExclusion {
    private final List<String> ALLOWED_ERRORS = new ArrayList<>();

    public boolean isErrorNotAllowed(final String errorMessage) {
        return ALLOWED_ERRORS
                .stream()
                .noneMatch(v -> Validator.matchValues(errorMessage, v));
    }

    public void init(final Scenario scenario) {
        final Map<String, String> errorsMap = ResourceRepository
                .getResourceAsMap(ResourceRepository.AvailableResource.CORRECT_ERROR_MESSAGE);

        // добавляем в исключения ошибки перечисленные в теге @errors: или @error:
        scenario.getSourceTagNames()
                .stream()
                .filter(tag -> tag.startsWith("@errors:") || tag.startsWith("@error:"))
                .flatMap(tag -> Stream.of(tag.split(":", 2)[1].split("[,;]")))
                .forEach(k -> {
                    if (!errorsMap.containsKey(k)) {
                        throw new AutotestError("В файле correct-error-message.resources нет ошибки с кодом " + k);
                    }
                    ALLOWED_ERRORS.add(errorsMap.get(k));
                });

        // добавляем в исключения все ошибки, которые начинаются с all_
        errorsMap.keySet()
                 .stream()
                 .filter(k -> k.startsWith("all_"))
                 .map(errorsMap::get)
                 .forEach(ALLOWED_ERRORS::add);
    }
}
