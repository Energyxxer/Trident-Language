package com.energyxxer.trident.compiler.semantics;

import java.util.HashMap;

public class SymbolTable {
    private final TridentFile file;

    private final HashMap<String, Symbol> symbols = new HashMap<>();

    public SymbolTable(TridentFile file) {
        this.file = file;
    }
}
