package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.ExceptionCollector;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
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

    default void putInContextForVisibility(Symbol.SymbolVisibility visibility, Symbol symbol) {
        this.put(symbol);
        if(visibility == Symbol.SymbolVisibility.GLOBAL) getGlobalContext().put(symbol);
    }

    default void assertLanguageLevel(int minLevel, String featureDesc, TokenPattern<?> pattern) {
        assertLanguageLevel(minLevel, featureDesc, pattern, null);
    }

    default void assertLanguageLevel(int minLevel, String featureDesc, TokenPattern<?> pattern, ExceptionCollector collector) {
        if(getStaticParentFile().getLanguageLevel() < minLevel) {
            TridentException x = new TridentException(TridentException.Source.LANGUAGE_LEVEL_ERROR, featureDesc + " only supported in language level " + minLevel + (minLevel < 3 ? " and above": ""), pattern, this);
            if(collector != null) {
                collector.log(x);
            } else {
                throw x;
            }
        }
    }
}
