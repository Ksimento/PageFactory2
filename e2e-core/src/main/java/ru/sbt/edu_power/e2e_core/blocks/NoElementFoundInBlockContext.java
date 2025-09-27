package ru.sbt.edu_power.e2e_core.blocks;

public class NoElementFoundInBlockContext extends RuntimeException {
    private static final long serialVersionUID = -8314054536729808863L;

    public NoElementFoundInBlockContext(final String message) {
        super(message);
    }
}
