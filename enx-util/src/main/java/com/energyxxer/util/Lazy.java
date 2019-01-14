package com.energyxxer.util;

import java.util.function.Consumer;

public class Lazy<T> {
    private T value = null;
    private final Factory<T> instantiator;
    private final Consumer<T> accessor;

    public Lazy(Factory<T> instantiator) {
        this(instantiator, null);
    }

    public Lazy(Factory<T> instantiator, Consumer<T> accessor) {
        this.instantiator = instantiator;
        this.accessor = accessor;
    }

    public T getValue() {
        if(value == null) value = instantiator.createInstance();
        if(accessor != null) accessor.accept(value);
        return value;
    }
}
