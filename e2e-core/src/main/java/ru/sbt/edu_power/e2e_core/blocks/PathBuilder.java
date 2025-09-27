package ru.sbt.edu_power.e2e_core.blocks;

import java.util.ArrayList;
import java.util.List;

public class PathBuilder {
    private final List<String> args = new ArrayList<>();

    public PathBuilder() {
    }

    public PathBuilder setBlock(final String blockName) {
        args.add(blockName);
        return this;
    }

    public PathBuilder setBlockNumber(final String blockNumber) {
        args.add(blockNumber);
        return this;
    }

    public PathBuilder setBlockNumber(final int blockNumber) {
        args.add(String.valueOf(blockNumber));
        return this;
    }

    public PathBuilder setBlockArgument(final String fieldName, final String value) {
        args.add(fieldName + "#" + value);
        return this;
    }

    public PathBuilder setField(final String fieldName) {
        args.add(fieldName);
        return this;
    }

    public String build() {
        return String.join("->", args);
    }
}
