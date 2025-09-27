package ru.sbt.edu_power.e2e_core.page_tags;

import ru.sbtqa.tag.pagefactory.Page;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Интерфейс реализует возможность указания родительской страницы.
 * По этим данным строится карта приложения с возможностью найти нужную страницу и её тег
 * При необходимости указать несколько родительских страниц, их нужно перечислить через
 * запятую в фигурных скобках
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ParentPage {
    Class<? extends Page>[] pageClass() default RootPage.class;
    boolean ignored() default false;
}
