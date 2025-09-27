package ru.sbt.sber_learning.test_runner;

import ru.sbt.edu_power.e2e_core.test_runner.TestListener;

public class Listener extends TestListener {
    protected Listener() {
        super(Configuration::configure);
    }
}
