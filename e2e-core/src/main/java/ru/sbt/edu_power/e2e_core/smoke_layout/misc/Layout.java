package ru.sbt.edu_power.e2e_core.smoke_layout.misc;

import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Layout {
    // Список разрешений под которыми должно выполняться тестирование верстки
    DimensionEnum[] dimension() default DimensionEnum.DESKTOP;

    // Список типов измерений, которые должны быть применены к элементу
    MeasuringTypes[] type() default MeasuringTypes.MULTITYPE;

    // Только для списочных элементов. Список номеров блоков, по которым нужно выполнить тестирование верстки
    int[] blocks() default 1;

    // Список вариантов наполнения данными
    DataVolume[] data() default DataVolume.NORMAL;

    // Требуется ли предварительная фильтрация данных перед выполнением теста верстки
    boolean filtered() default false;

    // Признак принудительно переопределяет дефолтные свойства
    // Например, когда нужно использовать значения по умолчанию
    // и в стандартном поведении они будут проигнорированы в пользу LayoutDefault значений
    boolean override() default false;
}
