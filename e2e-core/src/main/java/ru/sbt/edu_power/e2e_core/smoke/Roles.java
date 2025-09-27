package ru.sbt.edu_power.e2e_core.smoke;

import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.stream.Stream;

// Все доступные роли для использования в smoke тестах, разбитые по проектам
public enum Roles {
    // Для роутов ШЦП
    SYSADMIN_EDU("@R_sysadmin_edu", "sysadminEDU", false),
    CONFIGURATOR_EDU("@R_configurator_edu", "configuratorEDU", true),
    TEACHER_EDU("@R_teacher_edu", "teacherEDU", true),
    STUDENT_EDU("@R_student_edu", "studentEDU", true),
    PARENT_EDU("@R_parent_edu", "parentEDU", true),

    // Для роутов Школы 21
    SYSADMIN_S21("@R_sysadmin_s21", "sysadminS21", false),
    CONFIGURATOR_S21("@R_configurator_s21", "configuratorS21", true),
    TEACHER_S21("@R_teacher_s21", "teacherS21", true),
    STUDENT_S21("@R_student_s21", "studentS21", true),
    METHODOLOGY_S21("@R_methodology_s21", "methodologyS21", true),

    // Для роутов Bootcamp
    SYSADMIN_BTC("@R_sysadmin_btc", "sysadminBTC", false),
    CONFIGURATOR_BTC("@R_configurator_btc", "configuratorBTC", true),
    TEACHER_BTC("@R_teacher_btc", "teacherBTC", true),
    STUDENT_BTC("@R_student_btc", "studentBTC", true),

    // Для роутов MFE
    CONFIGURATOR_MFE("@R_configurator_mfe", "configuratorMFE", true),
    SYSADMIN_MFE("@R_sysadmin_mfe", "sysadminMFE", false),
    TEACHER_MFE("@R_teacher_mfe", "teacherMFE", true),
    STUDENT_MFE("@R_student_mfe", "studentMFE", true),
    PARENT_MFE("@R_parent_mfe", "parentMFE", true),

    // Для роутов Тарифы
    STUDENT_PMO_MFE("@R_student_pmo_mfe", "studentPmoMFE", true),
    STUDENT_LITE_MFE("@R_student_lite_mfe", "studentLiteMFE", true),
    PARENT_PMO_MFE("@R_parent_pmo_mfe", "parentPmoMFE", true),
    PARENT_LITE_MFE("@R_parent_lite_mfe", "parentLiteMFE", true),
    TEACHER_PMO_MFE("@R_teacher_pmo_mfe", "teacherPmoMFE", true),
    TEACHER_LITE_MFE("@R_teacher_lite_mfe", "teacherLiteMFE", true);

    private final String roleTag;
    private final String endPointName;
    private final boolean readyForProd;

    Roles(final String roleTag, final String endPointName, final boolean readyForProd) {
        this.roleTag = roleTag;
        this.endPointName = endPointName;
        this.readyForProd = readyForProd;
    }

    public String getRoleTag() {
        return roleTag;
    }

    public String getEndPointName() {
        return endPointName;
    }

    public boolean isReadyForProd() {
        return readyForProd;
    }

    public static Roles getRoleByTagName(final String tagName) {
        return Stream.of(Roles.values())
                .filter(role -> tagName.equals(role.getRoleTag()))
                .findFirst()
                .orElseThrow(() -> new AutotestError("Не найдено соответствие роли для " + tagName));
    }

    public static Roles getRoleByEndPointName(final String endPointName) {
        return Stream.of(Roles.values())
                .filter(role -> endPointName.equals(role.getEndPointName()))
                .findFirst()
                .orElseThrow(() -> new AutotestError("Не найдено соответствие роли для " + endPointName));
    }

    public String getProjectName() {
        final String [] splitValues =name().split("_");
        return splitValues[splitValues.length-1];
    }

    public Projects getProject() {
        return Projects.valueOf(getProjectName());
    }

    public String getRoleName() {
        return roleTag.split("_")[1];
    }
}
