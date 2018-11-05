package com.energyxxer.trident.ui.modules;

public interface ModuleTokenFactory<T extends ModuleToken> {
    T createFromIdentifier(String identifier);
}
