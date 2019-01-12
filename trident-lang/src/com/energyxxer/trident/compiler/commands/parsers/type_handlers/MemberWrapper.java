package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

public interface MemberWrapper<T> {
    Object unwrap(T instance);
}
