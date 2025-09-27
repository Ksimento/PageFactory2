package ru.sbt.edu_power.e2e_core.data;

import java.util.List;

public interface HasRows {
    boolean findFirstRow(final List<String> data);
    int getRowsSize();
}
