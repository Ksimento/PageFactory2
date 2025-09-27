package ru.sbt.edu_power.e2e_core.layout.repositories;

import io.qameta.allure.Allure;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class DataSetRepository {

    //    Метод возвращает мапу из названий полей репозитория и их значений
    Map<String, Object> getDataMap() {
        final Map<String, Object> dataMap = new LinkedHashMap<>();
        for (final Field field : this.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                dataMap.put(field.getName(), field.get(this));
            } catch (final IllegalAccessException e) {
                throw new AutotestError(e);
            }
        }
        return dataMap;
    }

    //    Метод выполняет сравнение элементов
    List<String> match(final DataSetRepository dataSet) {
        final List<String> allErrorsList = new ArrayList<>();
        final Map<String, Object> matching = dataSet.getDataMap();
        getDataMap().forEach((k, v) -> {
            if (!Validator.matchValues(matching.get(k), v)) {
                allErrorsList.add(String.format("%s: %s != %s",k, matching.get(k), v));
            }
        });
        return allErrorsList;
    }

    static String getMinifyedString(final String value) {
        return value.length() > 500 ? String.valueOf(value.hashCode()) : value;
    }

    @Override
    public String toString() {
        return getDataMap()
                .entrySet()
                .stream()
                .map(e -> "\t" + e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DataSetRepository && getClass().equals(obj.getClass())) {
            final Map<String, Object> expectedMap = getDataMap();
            final Map<String, Object> actualMap = ((DataSetRepository) obj).getDataMap();
            return expectedMap.keySet().stream().allMatch(name ->
                    {
                        if (actualMap.get(name) == null) {
                            Allure.addAttachment("Текущий дата-сет не содержит поля " + name, obj.toString());
                        }
                        if (expectedMap.get(name) == null) {
                            Allure.addAttachment("Записанный дата-сет не содержит поля " + name, this.toString());
                        }
                        return Validator.matchValues(actualMap.get(name), expectedMap.get(name));
                    }
            );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getDataMap()
                .values()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining()).hashCode();
    }
}
