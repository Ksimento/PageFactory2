package ru.sbt.edu_power.e2e_core.elements.ifaces;

import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;

public interface Fillable {
    void fillField(String value, Boolean validated) throws FieldFillingException;
}
