package ru.sbt.edu_power.risk_assessment;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Slf4j
public class SeparatorRules {
    private final LinkedIssueCollector linkedIssueCollector;
    private final LocalDateTime currentDate = LocalDateTime.now();
    private TCFields.Risk risk;
    private final Map<String, LongAdder> accum = new HashMap<>();

    private final Function<TestCaseModel, TCFields.Risk> ALL_TEST_YOUNGER_1_MONTH = tc ->
            tcBetween(1, 0, getTcDate(tc)) ? TCFields.Risk.CRITICAL : null;

    private final Function<TestCaseModel, TCFields.Risk> TEST_1_AND_3_MONTH_WO_DEF = tc -> {
        if (tcBetween(3, 1, getTcDate(tc)) && !tcHasDefect(tc)) {
            switch (getPriority(tc)) {
                case HIGH:
                    return TCFields.Risk.CRITICAL;
                case MEDIUM:
                    return TCFields.Risk.HIGH;
                case LOW:
                    return TCFields.Risk.MEDIUM;
                default:
                    throw new RiskAssessmentException("Нет обработчика для приоритета " + tc.getPriority().getName());
            }
        }
        return null;
    };

    private final Function<TestCaseModel, TCFields.Risk> TEST_3_AND_6_MONTH_WO_DEF = tc -> {
        if (tcBetween(6, 3, getTcDate(tc)) && !tcHasDefect(tc)) {
            switch (getPriority(tc)) {
                case HIGH:
                    return TCFields.Risk.HIGH;
                case MEDIUM:
                    return TCFields.Risk.MEDIUM;
                case LOW:
                    return TCFields.Risk.LOW;
                default:
                    throw new RiskAssessmentException("Нет обработчика для приоритета " + tc.getPriority().getName());
            }
        }
        return null;
    };

    private final Function<TestCaseModel, TCFields.Risk> TEST_OLDER_6_MONTH_WO_DEF = tc -> {
        if (tcBetween(999, 6, getTcDate(tc)) && !tcHasDefect(tc)) {
            switch (getPriority(tc)) {
                case HIGH:
                    return TCFields.Risk.MEDIUM;
                case MEDIUM:
                    return TCFields.Risk.LOW;
                case LOW:
                    return TCFields.Risk.LOWEST;
                default:
                    throw new RiskAssessmentException("Нет обработчика для приоритета " + tc.getPriority().getName());
            }
        }
        return null;
    };

    private final Function<TestCaseModel, TCFields.Risk> TEST_1_AND_3_MONTH_HAS_DEF = tc -> {
        if (tcBetween(3, 1, getTcDate(tc)) && tcHasDefect(tc)) {
            switch (getPriority(tc)) {
                case HIGH:
                case MEDIUM:
                    return TCFields.Risk.CRITICAL;
                case LOW:
                    return TCFields.Risk.HIGH;
                default:
                    throw new RiskAssessmentException("Нет обработчика для приоритета " + tc.getPriority().getName());
            }
        }
        return null;
    };

    private final Function<TestCaseModel, TCFields.Risk> ALL_TEST_WITH_DEF_YOUNGER_1_M = tc -> {
        if (defectBetween(1, 0, tc)) {
            switch (getPriority(tc)) {
                case HIGH:
                case MEDIUM:
                    return TCFields.Risk.CRITICAL;
                case LOW:
                    return TCFields.Risk.HIGH;
                default:
                    throw new RiskAssessmentException("Нет обработчика для приоритета " + tc.getPriority().getName());
            }
        }
        return null;
    };

    private final Function<TestCaseModel, TCFields.Risk> ALL_TEST_WITH_DEF_1_AND_3_MONTH = tc -> {
        if (defectBetween(3, 1, tc)) {
            switch (getPriority(tc)) {
                case HIGH:
                    return TCFields.Risk.CRITICAL;
                case MEDIUM:
                    return TCFields.Risk.HIGH;
                case LOW:
                    return TCFields.Risk.MEDIUM;
                default:
                    throw new RiskAssessmentException("Нет обработчика для приоритета " + tc.getPriority().getName());
            }
        }
        return null;
    };

    private final Function<TestCaseModel, TCFields.Risk> ALL_TEST_WITH_DEF_3_AND_6_MONTH = tc -> {
        if (defectBetween(6, 3, tc)) {
            switch (getPriority(tc)) {
                case HIGH:
                    return TCFields.Risk.CRITICAL;
                case MEDIUM:
                    return TCFields.Risk.MEDIUM;
                case LOW:
                    return TCFields.Risk.LOW;
                default:
                    throw new RiskAssessmentException("Нет обработчика для приоритета " + tc.getPriority().getName());
            }
        }
        return null;
    };

    private final Function<TestCaseModel, TCFields.Risk> ALL_TEST_WITH_DEF_OLDER_6_MONTH = tc -> {
        if (defectBetween(999, 6, tc)) {
            switch (getPriority(tc)) {
                case HIGH:
                    return TCFields.Risk.HIGH;
                case MEDIUM:
                    return TCFields.Risk.LOW;
                case LOW:
                    return TCFields.Risk.LOWEST;
                default:
                    throw new RiskAssessmentException("Нет обработчика для приоритета " + tc.getPriority().getName());
            }
        }
        return null;
    };

    public SeparatorRules(final LinkedIssueCollector linkedIssueCollector) {
        this.linkedIssueCollector = linkedIssueCollector;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public TCFields.Risk getRisk(final TestCaseModel testCaseModel) {
        if (testCaseModel.getHandleRiskManagement()) {
            if (!defectBetween(6, 0, testCaseModel)) {
                return null;
            }
        }

        for (final Field field : this.getClass().getDeclaredFields()) {
            if (Function.class.isAssignableFrom(field.getType())) {
                risk = ((Function<TestCaseModel, TCFields.Risk>) field.get(this)).apply(testCaseModel);
                if (Objects.nonNull(risk)) {
                    if (!accum.containsKey(field.getName())) {
                        accum.put(field.getName(), new LongAdder());
                    }
                    accum.get(field.getName()).increment();
                    return risk;
                }
            }
        }
        log.error("Тест-кейс не удовлетворяет ни одному условию: {}", testCaseModel.getKey());
        return null;
    }

    public Map<String, LongAdder> getAccum() {
        return accum;
    }

    private TCFields.Priority getPriority(final TestCaseModel testCaseModel) {
        return TCFields.Priority.getByValue(testCaseModel.getPriority().getName(), testCaseModel.getKey());
    }

    private boolean tcBetween(final int minusMonthFrom, final int minusMonthTo, final LocalDateTime tcDate) {
        return tcDate.isAfter(currentDate.minusMonths(minusMonthFrom)) && tcDate.isBefore(currentDate.minusMonths(minusMonthTo));
    }

    private boolean tcHasDefect(final TestCaseModel testCaseModel) {
        return linkedIssueCollector.checkIfTestHasDefect(testCaseModel.getKey());
    }

    private boolean defectBetween(final int minusMonthFrom, final int minusMonthTo, final TestCaseModel testCaseModel) {
        return linkedIssueCollector.checkIfTestHasDefectInMonthRange(minusMonthFrom, minusMonthTo, testCaseModel.getKey());
    }

    private LocalDateTime getTcDate(final TestCaseModel testCaseModel) {
        return new Timestamp(testCaseModel.getCreatedOn().getTime()).toLocalDateTime();
    }

    public enum Rules {
        ALL_TEST_YOUNGER_1_MONTH("Все тест-кейсы младше 1 месяца"),
        TEST_1_AND_3_MONTH_WO_DEF("Тест-кейсы созданные от 1 до 3 месяцев назад без дефектов"),
        TEST_3_AND_6_MONTH_WO_DEF("Тест-кейсы созданные от 3 до 6 месяцев назад без дефектов"),
        TEST_OLDER_6_MONTH_WO_DEF("Тест-кейсы старше 6 месяцев без дефектов"),
        TEST_1_AND_3_MONTH_HAS_DEF("Тест-кейсы созданные от 1 до 3 месяцев назад с дефектами"),
        ALL_TEST_WITH_DEF_YOUNGER_1_M("Все тест-кейсы с дефектами созданными в течении последнего месяца"),
        ALL_TEST_WITH_DEF_1_AND_3_MONTH("Все тест-кейсы с дефектами созданными от 1 до 3 месяцев назад"),
        ALL_TEST_WITH_DEF_3_AND_6_MONTH("Все тест-кейсы с дефектами созданными от 3 до 6 месяцев назад"),
        ALL_TEST_WITH_DEF_OLDER_6_MONTH("Все тест-кейсы с дефектами старше 6 месяцев");

        private final String description;

        Rules(final String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
