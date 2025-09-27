package ru.sbt.edu_power.e2e_core.smoke_layout.misc;

import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LayoutDefault {
    // Список разрешений под которыми должно выполняться тестирование верстки
    DimensionEnum[] dimension() default DimensionEnum.DESKTOP;

    // Список типов измерений, которые должны быть применены к элементу
    MeasuringTypes[] type() default MeasuringTypes.MULTITYPE;

    // Список вариантов наполнения данными
    DataVolume[] data() default DataVolume.NORMAL;

    // Требуется ли предварительная фильтрация данных перед выполнением теста верстки
    boolean filtered() default false;
}
