package com.energyxxer.util;

import java.util.function.Consumer;

public class Lazy<T> {
    private boolean hasValue = false;
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
        if(!hasValue) {
            value = instantiator.createInstance();
            hasValue = true;
        }
        if(accessor != null) accessor.accept(value);
        return value;
    }

    public boolean hasValue() {
        return hasValue;
    }
}
