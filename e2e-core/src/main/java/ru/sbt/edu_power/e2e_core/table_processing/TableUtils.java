package ru.sbt.edu_power.e2e_core.table_processing;

import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

class TableUtils {
    static Field getFieldByName(final String name) {
        return getFieldByName(PageContext.getCurrentPage().getClass(), name);
    }

    static Field getFieldByName(final Class context, final String name) {
        for (final Field field : context.getFields()) {
            if (field.isAnnotationPresent(ElementTitle.class)
                && field.getAnnotation(ElementTitle.class).value().equals(name)) {
                return field;
            }
        }
        throw new AutotestError(String.format(
                "Поле \"%s\" не задекларировано в классе \"%s\"",
                name,
                context.getName()
        ));
    }

    static String getXpath(final Field field, final Class<? extends Annotation> annotation) {
        Assert.assertTrue(
                String.format(
                        "Поле \"%s\" не содержит аннотации \"%s\"",
                        field.getAnnotation(ElementTitle.class).value(), annotation.getSimpleName()
                ), field.isAnnotationPresent(annotation));
        final String xpath;
        if (annotation.isAssignableFrom(FindTableCell.class)) {
            xpath = field.getAnnotation(FindTableCell.class).xpath();
        } else if (annotation.isAssignableFrom(FindTableHeaderCol.class)) {
            xpath = field.getAnnotation(FindTableHeaderCol.class).xpath();
        } else {
            throw new AutotestError(String.format("Аннотация \"%s\" не поддерживается", annotation.getSimpleName()));
        }
        return xpath;
    }

    static String prepareColumnName(final String columnName) {
        final String preparedName;
        if (DriverUtils.empty(columnName)) {
            preparedName = "empty";
        } else if ("true".equalsIgnoreCase(columnName) || "false".equalsIgnoreCase(columnName)) {
            preparedName = "checkbox";
        } else {
            preparedName = columnName.trim().toLowerCase();
        }
        return preparedName;
    }
}
