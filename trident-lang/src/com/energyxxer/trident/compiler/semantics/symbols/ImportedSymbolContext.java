package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.constructs.ActualParameterList;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class ImportedSymbolContext implements ISymbolContext {
    protected final TridentCompiler compiler;
    private ArrayList<ISymbolContext> contexts = new ArrayList<>();
    private ISymbolContext parent = null;

    public ImportedSymbolContext(TridentCompiler compiler) {
        this.compiler = compiler;
    }


    public void addContext(ISymbolContext ctx) {
        contexts.add(ctx);
    }

    @Override
    public Symbol search(@NotNull String name, ISymbolContext from, ActualParameterList params) {
        for(ISymbolContext ctx : contexts) {
            Symbol result = ctx.search(name, from, params);
            if(result != null) return result;
        }
        if(parent != null) return parent.search(name, from, params);
        return getGlobalContext().search(name, from, params);
    }

    @Override
    public @NotNull TridentCompiler getCompiler() {
        return compiler.getRootCompiler();
    }

    @Override
    public TridentFile getStaticParentFile() {
        throw new IllegalStateException();
    }

    @Override
    public void put(Symbol symbol) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ISymbolContext getParent() {
        return parent;
    }

    public void setParentScope(ISymbolContext parent) {
        this.parent = parent;
    }

    @Override
    public HashMap<String, Symbol> collectVisibleSymbols(HashMap<String, Symbol> list, ISymbolContext from) {
        for(ISymbolContext ctx : contexts) {
            ctx.collectVisibleSymbols(list, from);
        }
        if(parent != null) parent.collectVisibleSymbols(list, from);
        return list;
    }
}
