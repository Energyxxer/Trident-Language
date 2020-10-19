package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.prismarine.typesystem.MemberWrapper;

public class FieldWrapper<T> implements MemberWrapper<T> {
    public interface Picker<T> {

        Object select(T instance);
    }
    private final Picker<T> picker;

    public FieldWrapper(Picker<T> picker) {
        this.picker = picker;
    }

    @Override
    public Object unwrap(T instance) {
        return picker.select(instance);
    }

}
