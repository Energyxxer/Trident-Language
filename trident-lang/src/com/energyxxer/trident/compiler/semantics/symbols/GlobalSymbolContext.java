package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.constructs.ActualParameterList;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

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
    public Symbol search(@NotNull String name, ISymbolContext from, ActualParameterList params) {
        return map.get(name);
    }

    @Override
    public @NotNull TridentCompiler getCompiler() {
        return compiler.getRootCompiler();
    }

    @Override
    public TridentFile getStaticParentFile() {
        return null;
    }

    @Override
    public void put(Symbol symbol) {
        map.put(symbol.getName(), symbol);
    }

    public void join(GlobalSymbolContext other) {
        map.putAll(other.map);
    }

    @Override
    public HashMap<String, Symbol> collectVisibleSymbols(HashMap<String, Symbol> list, ISymbolContext from) {
        for(Map.Entry<String, Symbol> entry : map.entrySet()) {
            list.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return map;
    }
}
