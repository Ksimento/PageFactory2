package ru.sbt.edu_power.e2e_core.error_processing;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.e2e_core.test_runner.ScenarioDataStorage;
import ru.sbt.edu_power.external_services.shared.fail_categories.FailCategoriesCore;
import ru.sbt.edu_power.external_services.shared.fail_categories.IFailCategories;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.pagefactory.exceptions.AllureNonCriticalError;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Класс реализует возможность аккумуляции некритичных ошибок, появление которых
 * не должно останавливать тест, но должно пометить шаг и весь тест сбойным по его окончанию
 * В процессе выполнения ошибки аккумулируются и выдаются все по окончанию теста.
 * Так же в каждый
 */
@Slf4j
public class NotCriticalErrorAccumulator {
    private static final String SCENARIO_ERRORS = "SCENARIO_ERRORS";
    private static final String STEP_ERRORS = "STEP_ERRORS";

    public static void setNotCriticalError(final String message) {
        setNotCriticalError(message, message);
    }

    public static void setNotCriticalError(final String message, final String trace) {
        setNotCriticalError(message, FailCategoriesCore.determineOrNull(ScenarioDataStorage.PROJECT, trace));
    }

    public static void setNotCriticalError(final String message, final IFailCategories failCategory) {
        insertError(SCENARIO_ERRORS, message);
        final String messageWithCategory = Objects.isNull(failCategory) ? message : message + ". " + failCategory.getName();
        insertError(STEP_ERRORS, messageWithCategory);
    }

    private static void insertError(final String errorType, final String message) {
        if (!Stash.asMap().containsKey(errorType)) {
            Stash.put(errorType, new ArrayList<>());
        }
        final List<String> errors = Stash.getValue(errorType);
        errors.add(message);
    }

    public static void throwErrors() {
        if (!Stash.asMap().containsKey(SCENARIO_ERRORS)) {
            return;
        }
        final List<String> errors = Stash.getValue(SCENARIO_ERRORS);
        if (errors != null && !errors.isEmpty()) {
            final String message = String.join("\n", errors);
            Stash.remove(SCENARIO_ERRORS);
            throw new AutotestError("Во время прохождения теста были выявлены некритичные ошибки: \n" + message);
        }
    }

    public static void setStepBroken() {
        if (!Stash.asMap().containsKey(STEP_ERRORS)) {
            return;
        }
        final List<String> errors = Stash.getValue(STEP_ERRORS);
        if (errors != null && !errors.isEmpty()) {
            final String message = String.join("\n", errors);
            Stash.remove(STEP_ERRORS);
            throw new AllureNonCriticalError(message);
        }
    }
}
