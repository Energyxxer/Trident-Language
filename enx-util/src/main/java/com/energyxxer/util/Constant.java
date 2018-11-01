package com.energyxxer.util;

public class Constant {
    private final String name;
    private final Object group;

    public Constant(String name) {
        this(name, null);
    }

    public Constant(String name, Object group) {
        this.name = name;
        this.group = group;
    }

    public boolean isOfGroup(Object group) {
        return this.group == group;
    }

    @Override
    public String toString() {
        return name;
    }
}
