package ru.sbt.edu_power.e2e_core.elements.ifaces;

import java.util.Optional;

public interface Warning {
    Optional<String> getTextWarning();
    void notTextWarning();
}
