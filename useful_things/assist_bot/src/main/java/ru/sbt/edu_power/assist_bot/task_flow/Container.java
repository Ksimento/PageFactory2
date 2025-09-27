package ru.sbt.edu_power.assist_bot.task_flow;

public class Container<T> {
    private T object;

    public Container() {
    }

    public Container(final T object) {
        this.object = object;
    }

    public T getObject() {
        return object;
    }

    public void setObject(final T object) {
        this.object = object;
    }
}
