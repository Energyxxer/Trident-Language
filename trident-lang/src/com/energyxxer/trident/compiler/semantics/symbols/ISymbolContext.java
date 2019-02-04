package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

public interface ISymbolContext {

    ISymbolContext getParent();

    Symbol search(@NotNull String name, ISymbolContext from);

    @NotNull TridentCompiler getCompiler();

    TridentFile getStaticParentFile();

    void put(Symbol symbol);

    default TridentFile getWritingFile() {
        return getCompiler().getWritingFile();
    }

    default ISymbolContext getGlobalContext() {
        return getCompiler().getGlobalContext();
    }

    default ISymbolContext getContextForVisibility(Symbol.SymbolVisibility visibility) {
        if(visibility == Symbol.SymbolVisibility.GLOBAL) return getGlobalContext();
        return this;
    }
}
