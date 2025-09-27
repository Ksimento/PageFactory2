package ru.sbt.edu_power.e2e_core.smoke_layout;

import ru.sbt.edu_power.e2e_core.smoke.Projects;
import ru.sbt.edu_power.e2e_core.smoke.Roles;

public class SLTFeaturesSteps {
    private static final String AUTH_EDU = "И авторизуется на сайте под учетной записью \"%s\"";
    private static final String AUTH_MFE = "И авторизуется под учеником \"%s\"";
    private static final String AUTH_STUDENT_S21 = "И переходит на новый дизайн авторизуясь под студентом \"%s\"";
    private static final String AUTH_ADMIN_S21 = "И авторизуется под учетной записью админки \"%s\"";
    private static final String LAYOUT_STEP_SIMPLE = "И проверяет параметры верстки. Путь \"%path\". Сценарий \"%scenario\"";
    private static final String LAYOUT_STEP_SELF_CONTAINER = "И проверяет параметры верстки относительно контейнера. Путь \"%path\". Сценарий \"%scenario\"";
    private static final String FILTER_STEP = "#!filter_Start_%s\n\n#!filter_End_%s";
    private static final String FUNCTIONAL = "Функционал: %s";
    private static final String SCENARIO = "Сценарий: %s";
    private static final String LANG = "#language:ru";
    private static final String REFRESH_PAGE = "И обновляет страницу с контролем загрузки";

    public String getAuthStep(final Roles role) {
        final String project = role.getProjectName();
        if (project.equals(Projects.MFE.name())) {
            return AUTH_MFE;
        } else if (project.equals(Projects.S21.name())) {
            if (role == Roles.STUDENT_S21) {
                return AUTH_STUDENT_S21;
            } else if (role == Roles.CONFIGURATOR_S21 || role == Roles.SYSADMIN_S21) {
                return AUTH_ADMIN_S21;
            }
        }
        return AUTH_EDU;
    }

    public String getFilterStep() {
        return FILTER_STEP;
    }

    public String getLang() {
        return LANG;
    }

    public String getLayoutStepSimple() {
        return LAYOUT_STEP_SIMPLE;
    }

    public String getLayoutStepSelfContainer() {
        return LAYOUT_STEP_SELF_CONTAINER;
    }

    public String getFunctional() {
        return FUNCTIONAL;
    }

    public String getScenario() {
        return SCENARIO;
    }

    public String getRefreshPage() {
        return REFRESH_PAGE;
    }
}
