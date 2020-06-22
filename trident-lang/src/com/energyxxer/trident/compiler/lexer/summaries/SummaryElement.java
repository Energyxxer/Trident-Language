package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.semantics.Symbol;

import java.util.ArrayList;
import java.util.function.Function;

public interface SummaryElement {
    String getName();
    int getStartIndex();
    int getEndIndex();

    void putElement(SummaryElement element);

    SummaryModule getParentFileSummary();

    void collectGlobalSymbols(ArrayList<SummarySymbol> list);

    void updateIndices(Function<Integer, Integer> h);

    void collectSubSymbolsForPath(String[] path, int pathStart, ArrayList<SummarySymbol> list, TridentUtil.ResourceLocation fromFile, int inFileIndex);

    default void collectSymbolsVisibleAt(int index, ArrayList<SummarySymbol> list, boolean fromSameFile) {

    }

    default Symbol.SymbolVisibility getVisibility() {
        return Symbol.SymbolVisibility.LOCAL;
    }
}
