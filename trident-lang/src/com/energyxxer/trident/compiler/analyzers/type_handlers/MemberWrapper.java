package com.energyxxer.trident.compiler.analyzers.type_handlers;

public interface MemberWrapper<T> {
    Object unwrap(T instance);
}
