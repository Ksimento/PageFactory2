package ru.sbt.edu_power.external_services.validator;

public interface Validatable {
    String getFieldValue();
    boolean validate(String expected);
}
