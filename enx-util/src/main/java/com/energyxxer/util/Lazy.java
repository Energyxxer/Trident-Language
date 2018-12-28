package com.energyxxer.util;

public class Lazy<T> {
    private T value = null;
    private final Factory<T> instantiator;

    public Lazy(Factory<T> instantiator) {
        this.instantiator = instantiator;
    }

    public T getValue() {
        if(value == null) value = instantiator.createInstance();
        return value;
    }
}
