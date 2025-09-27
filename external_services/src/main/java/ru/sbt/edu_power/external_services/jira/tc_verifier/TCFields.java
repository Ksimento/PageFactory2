package ru.sbt.edu_power.external_services.jira.tc_verifier;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;

import java.util.stream.Stream;

@Slf4j
public enum TCFields {
    AUTOMATED_STATUS("Автоматизирован", "Автоматизирован", true),
    FRAMEWORK("Framework", "Framework", true),
    TEST_TYPE("Тип теста", "Тип теста", true),
    TEST_VIEW("Вид теста", "Вид теста", true),
    AUTOMATOR("Автоматизатор", "Автоматизатор", true),
    TEAM("Team", "Team", true),
    STATUS("status", "statusName", false),
    LABELS("labels", "labelName", false),
    ARCHIVED("archived", "archived", false),
    PROJECT_ID("projectId", "projectId", false),
    NOT_AUTOMATED_REASON("Причина невозможности автоматизации", "Причина невозможности автоматизации", true),
    FOLDER("folder", "folderTreeId", false),
    IGNORED("Игнорируем поле", "Игнорируем поле", true),
    PRIORITY("priority", "priorityName", false),
    OBJECTIVE("objective", "objective", false),
    ISSUE_LINKS("issueLinks", "issueLinks", false),
    RISK("Риск", "Риск", true),
    LAYOUT_DESKTOP("Десктоп верстка", "Десктоп верстка", true),
    LAYOUT_TABLET_LANDSCAPE("Планшет (альбом.) верстка", "Планшет (альбом.) верстка", true),
    LAYOUT_TABLET_PORTRAIT("Планшет (книжн.) верстка", "Планшет (книжн.) верстка", true),
    LAYOUT_MOBILE_PORTRAIT("Мобильный", "Мобильный", true),
    HANDLE_RISK_MANAGEMENT("Ручное управление риском", "Ручное управление риском", true),
    ESTIMATED_TIME("estimatedTime", "Расчтёное время", false),
    KEY_NAME("keyName", "keyName", false); // поле используется только для поиска тест-кейса через фильтр

    public final String value;
    public final String filterName;
    public final boolean isCustom;

    TCFields(final String value, final String filterName, final boolean isCustom) {
        this.value = value;
        this.filterName = filterName;
        this.isCustom = isCustom;
    }

    public static TCFields getFieldByName(final String fieldName) {
        return Stream.of(TCFields.values())
                     .filter(f -> f.value.equals(fieldName))
                     .findFirst()
                     .orElse(IGNORED);
    }

    public static TCFields getFieldByFilterName(final String filterName) {
        return Stream.of(TCFields.values())
                     .filter(f -> f.filterName.equals(filterName))
                     .findFirst()
                     .orElse(IGNORED);
    }

    public enum AutomatedStatus {
        NOT_REQUIRED("0 - Не требуется"),
        YES("1 - Да"),
        NOT("2 - Нет"),
        ON_AUTOMATE("3 - На автоматизации"),
        EXCLUDED("4 - Отключен"),
        ARCHIVED("Архивированный статус");

        public final String value;

        AutomatedStatus(final String value) {
            this.value = value;
        }

        public static AutomatedStatus getByValue(final String value, final String key) {
            return Stream.of(AutomatedStatus.values())
                         .filter(s -> s.value.equals(value))
                         .findFirst()
                         .orElse(ARCHIVED);
        }
    }

    public enum Framework {
        SELENIUM("Selenium"),
        JS("JS"),
        SELENIUM_JS("Selenium+JS"),
        EMPTY("Empty");

        public final String value;

        Framework(final String value) {
            this.value = value;
        }

        public static Framework getByValue(final String value) {
            return Stream.of(Framework.values())
                         .filter(v -> v.value.equals(value))
                         .findFirst()
                         .orElse(EMPTY);
        }
    }

    public enum TestType {
        N_A("N/A"),
        NOT_FOR_REGRESS("Не для регресса"),
        N_F("НФ"),
        REGRESS("Регресс"),
        ARCHIVED("Архивированный тип теста");

        public final String value;

        TestType(final String value) {
            this.value = value;
        }

        public static TestType getByValue(final String value, final String key) {
            return Stream.of(TestType.values())
                         .filter(s -> s.value.equals(value))
                         .findFirst()
                         .orElse(ARCHIVED);
        }
    }

    public enum TestView {
        U_I("UI"),
        LAYOUT("Верстка"),
        API("API"),
        ARCHIVED("Архивированный вид теста");

        public final String value;

        TestView(final String value) {
            this.value = value;
        }

        public static TestView getByValue(final String value, final String key) {
            return Stream.of(TestView.values())
                         .filter(s -> s.value.equals(value))
                         .findFirst()
                         .orElse(ARCHIVED);
        }
    }

    public enum Status {
        APPROVED("Approved"),
        DRAFT("Draft"),
        DEPRECATED("Deprecated"),
        DISABLED("Disabled"),
        NEED_REFACTORING("Need Refactoring");

        public final String value;

        Status(final String value) {
            this.value = value;
        }

        public static Status getByValue(final String value, final String key) {
            return Stream.of(Status.values())
                         .filter(s -> s.value.equals(value))
                         .findFirst()
                         .orElseThrow(() -> new JiraConnectionException("Не установлен статус для кейса " + key));
        }
    }

    public enum ProjectId {
        EDU(10101, "EDUPOWER"),
        MFE(10101, "EDUPOWER"),
        S21(10601, "S21"),
        ACC(10709, "ACC"),
        UP(10602, "UP"),
        DASH(11502, "EDUPOWER"),
        PROF(11506, "EDUPOWER"),
        B2C(11532, "B2C"),
        BTC(10203, "Bootcamp"),
        LIT(11001,"SberSova");

        public final Integer id;
        public final String confluenceKey;

        ProjectId(final Integer id, final String confluenceKey) {
            this.id = id;
            this.confluenceKey = confluenceKey;
        }

        public static ProjectId getById(final Integer projectId) {
            return Stream.of(ProjectId.values())
                         .filter(p -> p.id.equals(projectId))
                         .findFirst()
                         .orElseThrow(() -> new JiraConnectionException("Проект не найден по ID " + projectId));
        }

        public static ProjectId getByIdOrNull(final Integer projectId) {
            return Stream.of(ProjectId.values())
                         .filter(p -> p.id.equals(projectId))
                         .findFirst()
                         .orElse(null);
        }
    }

    public enum Priority {
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low");

        public final String value;

        Priority(final String value) {
            this.value = value;
        }

        public static Priority getByValue(final String value, final String key) {
            return Stream.of(Priority.values())
                         .filter(s -> s.value.equals(value))
                         .findFirst()
                         .orElseThrow(() -> new JiraConnectionException("Не установлен приоритет для кейса " + key));
        }
    }

    public enum Risk {
        UNDEFINED("0 - Не задан"),
        LOWEST("1 - Минимальный"),
        LOW("2 - Низкий"),
        MEDIUM("3 - Средний"),
        HIGH("4 - Высокий"),
        CRITICAL("5 - Критический");

        public final String value;

        Risk(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Risk getByValue(final String value, final String key) {
            return Stream.of(Risk.values())
                         .filter(s -> s.value.equals(value))
                         .findFirst()
                         .orElse(UNDEFINED);
        }

        public static Risk getByName(final String name) {
            return Stream.of(Risk.values())
                    .filter(s -> s.name().equals(name))
                    .findFirst()
                    .orElse(UNDEFINED);
        }
    }

    public enum Layout {
        UNDEFINED(""),
        YES("Да"),
        NOT("Нет"),
        NOT_REQUIRED("Не требуется");
        public final String value;

        Layout(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Layout getByValue(final String value, final String key) {
            return Stream.of(Layout.values())
                         .filter(s -> s.value.equals(value))
                         .findFirst()
                         .orElse(UNDEFINED);
        }
    }
}
