package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class GlobalSymbolContext implements ISymbolContext {
    private final TridentCompiler compiler;
    private HashMap<String, Symbol> map = new HashMap<>();

    public GlobalSymbolContext(TridentCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public ISymbolContext getParent() {
        return null;
    }

    @Override
    public Symbol search(@NotNull String name, ISymbolContext from) {
        return map.get(name);
    }

    @Override
    public @NotNull TridentCompiler getCompiler() {
        return compiler;
    }

    @Override
    public TridentFile getStaticParentFile() {
        return null;
    }

    @Override
    public void put(Symbol symbol) {
        map.put(symbol.getName(), symbol);
    }
}
