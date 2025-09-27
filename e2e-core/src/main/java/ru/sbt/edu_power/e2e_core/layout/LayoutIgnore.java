package ru.sbt.edu_power.e2e_core.layout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Аннотация для указания xpath элемента, содержащего меняющийся текст
// Xpath должен быть указан от текущего элемента (начиная с точки)
// При выполнении снапшота экрана для тестов верстки, текст этого элемента в снапшоте будет заменён на звёздочку
// и таким образом фреймворк не будет реагировать на изменение контента элемента
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface LayoutIgnore {
    String[] xpath();
}
