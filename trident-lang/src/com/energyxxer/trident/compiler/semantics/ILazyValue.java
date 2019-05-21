package com.energyxxer.trident.compiler.semantics;

public interface ILazyValue {
    <T> T getValue(Class<T> expected);
}
