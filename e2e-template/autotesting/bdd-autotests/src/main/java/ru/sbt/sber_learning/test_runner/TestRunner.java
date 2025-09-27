package ru.sbt.sber_learning.test_runner;

import cucumber.api.junit.Cucumber;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

public class TestRunner extends Cucumber {
    private final Listener listener;

    public TestRunner(final Class clazz) throws InitializationError {
        super(clazz);
        listener = new Listener();
    }

    @Override
    public void run(final RunNotifier notifier) {
        notifier.addListener(listener);
        notifier.fireTestRunStarted(getDescription());
        super.run(notifier);
    }
}
