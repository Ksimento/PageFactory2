package ru.sbt.edu_power.e2e_core.error_processing;

import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollector {
    private final String ERROR_LIST_NAME = "ErrorCollector";

    public void assertTrue(final String message, final boolean expression) {
        if (!expression) {
            add(message);
        }
    }

    public void assertFalse(final String message, final boolean expression) {
        if (expression) {
            add(message);
        }
    }

    public void assertEquals(final String message, final Object actual, final Object expected) {
        if (!actual.equals(expected)) {
            add(message);
        }
    }

    public void assertNotEquals(final String message, final Object actual, final Object expected) {
        if (actual.equals(expected)) {
            add(message);
        }
    }

    public void assertNotNull(final String message, final Object obj) {
        if (null == obj) {
            add(message);
        }
    }

    private void add(final String message) {
        if (!Stash.asMap().containsKey(ERROR_LIST_NAME)) {
            Stash.put(ERROR_LIST_NAME, new ArrayList<>());
        }
        final List<String> errorList = Stash.getValue(ERROR_LIST_NAME);
        errorList.add(message);
    }

    public void assertAll() {
        if (Stash.asMap().containsKey(ERROR_LIST_NAME)) {
            final List<String> errorList = Stash.getValue(ERROR_LIST_NAME);
            if (!errorList.isEmpty()) {
                throw new AutotestError("\n" + String.join("\n", errorList));
            }
        }
    }
}
