package com.energyxxer.trident.compiler.lexer.summaries;

import java.util.function.Function;

public interface SummaryElement {
    String getName();
    int getStartIndex();
    int getEndIndex();

    void putElement(SummaryElement element);

    void updateIndices(Function<Integer, Integer> h);
}
