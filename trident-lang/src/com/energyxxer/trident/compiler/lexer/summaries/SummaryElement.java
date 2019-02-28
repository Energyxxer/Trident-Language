package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;

import java.util.function.Function;

public interface SummaryElement {
    String getName();
    int getStartIndex();
    int getEndIndex();

    void putElement(SummaryElement element);

    SummaryModule getParentFileSummary();

    void updateIndices(Function<Integer, Integer> h);
}
