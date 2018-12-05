package com.energyxxer.trident.compiler.semantics;

import java.util.HashMap;

public class SymbolTable {
    private final TridentFile file;

    private final HashMap<String, Symbol> symbols = new HashMap<>();

    public SymbolTable(TridentFile file) {
        this.file = file;
    }

    public int size() {
        return symbols.size();
    }

    public boolean isEmpty() {
        return symbols.isEmpty();
    }

    public Symbol get(String key) {
        return symbols.get(key);
    }

    public boolean containsKey(String key) {
        return symbols.containsKey(key);
    }

    public Symbol put(Symbol symbol) {
        return symbols.put(symbol.getName(), symbol);
    }

    public Symbol remove(String key) {
        return symbols.remove(key);
    }

    public void clear() {
        symbols.clear();
    }
}
