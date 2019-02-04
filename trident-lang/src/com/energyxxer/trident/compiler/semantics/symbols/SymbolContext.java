package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class SymbolContext implements ISymbolContext {
    protected final TridentCompiler compiler;
    protected ISymbolContext parentScope;
    private HashMap<String, Symbol> table = new HashMap<>();

    public SymbolContext(TridentCompiler compiler) {
        this.compiler = compiler;
    }

    public SymbolContext(ISymbolContext parentScope) {
        this.compiler = parentScope.getCompiler();
        this.parentScope = parentScope;
    }

    public Symbol search(@NotNull String name, ISymbolContext from) {
        Symbol inMap = table.get(name);
        if(inMap != null &&
                (inMap.getVisibility() != Symbol.SymbolVisibility.PRIVATE || from == this ||
                        this.getStaticParentFile().getPattern().getFile().equals(from.getStaticParentFile().getPattern().getFile())
                )
        ) {
            return table.get(name);
        }
        else if(parentScope != null) return parentScope.search(name, from);
        else return getGlobalContext().search(name, from);
    }

    public @NotNull TridentCompiler getCompiler() {
        return compiler;
    }

    public TridentFile getStaticParentFile() {
        if(this instanceof TridentFile) return ((TridentFile) this);
        else if(parentScope != null) return parentScope.getStaticParentFile();
        else throw new IllegalStateException();
    }

    public void put(Symbol symbol) {
        table.put(symbol.getName(), symbol);
    }

    @Override
    public ISymbolContext getParent() {
        return parentScope;
    }
}
