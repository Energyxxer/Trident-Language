package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.trident.compiler.semantics.Symbol;

import java.util.ArrayList;
import java.util.function.Function;

public interface SummaryElement {
    String getName();
    int getStartIndex();
    int getEndIndex();

    void putElement(SummaryElement element);

    SummaryModule getParentFileSummary();

    void updateIndices(Function<Integer, Integer> h);

    default void collectSymbolsVisibleAt(int index, ArrayList<SummarySymbol> list, boolean fromSameFile) {

    }

    default Symbol.SymbolVisibility getVisibility() {
        return Symbol.SymbolVisibility.LOCAL;
    }

    void collectSubSymbolsForPath(String[] path, int pathStart, ArrayList<SummarySymbol> list);
}
