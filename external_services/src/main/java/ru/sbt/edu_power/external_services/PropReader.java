package ru.sbt.edu_power.external_services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Класс реализует возможность доступа к наборам настроек, хранимых в файле конфигурации application.properties
public class PropReader {
    private static final Properties PROPERTIES = new Properties();

    public static String get(final String propName) {
        if (!PROPERTIES.containsKey(propName)) {
            throw new ExternalServicesException("Не найдена проперти для " + propName);
        }
        return PROPERTIES.getProperty(propName);
    }

    public static void set(final String propName, final String value) {
        PROPERTIES.put(propName, value);
    }

    static {
        try(final InputStream inputStream = PropReader.class.getClassLoader().getResourceAsStream("application.properties")) {
            PROPERTIES.load(inputStream);
        } catch (final IOException e) {
            throw new ExternalServicesException(e);
        }
    }


}
