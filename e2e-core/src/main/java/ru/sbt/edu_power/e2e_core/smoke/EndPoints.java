package ru.sbt.edu_power.e2e_core.smoke;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Интерфейс добавляется к страницам pageObject для указания эндпоинтов для этой страницы к каждой используемой роли
 * Если в эндпоинте есть параметры, они должны быть заменены плейсхолдером %s
 * Пример:
 *
 * @EndPoints {
 *     teacherEDU = "/path/%s/second/%s",
 *     teacherS21 = "/path/%s/second/%s"
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EndPoints {
    // Для роутов ШЦП
    String sysadminEDU() default "";
    String configuratorEDU() default "";
    String teacherEDU() default "";
    String studentEDU() default "";
    String parentEDU() default "";

    // Для роутов Школы 21
    String sysadminS21() default "";
    String configuratorS21() default "";
    String teacherS21() default "";
    String studentS21() default "";
    String methodologyS21() default "";

    // Для роутов Bootcamp
    String sysadminBTC() default "";
    String configuratorBTC() default "";
    String teacherBTC() default "";
    String studentBTC() default "";

    // Для роутов MFE
    String sysadminMFE() default "";
    String configuratorMFE() default "";
    String teacherMFE() default "";
    String studentMFE() default "";
    String parentMFE() default "";

    // Для роутов Тарифы
    String studentPmoMFE() default "";
    String studentLiteMFE() default "";
    String parentPmoMFE() default "";
    String parentLiteMFE() default "";
    String teacherPmoMFE() default "";
    String teacherLiteMFE() default "";

    boolean ignored() default false;
}
