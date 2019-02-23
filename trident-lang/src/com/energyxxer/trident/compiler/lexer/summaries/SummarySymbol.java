package com.energyxxer.trident.compiler.lexer.summaries;

import java.util.ArrayList;
import java.util.function.Function;

public class SummarySymbol implements SummaryElement {
    private String name;
    private int declarationIndex;
    private ArrayList<String> suggestionTags = new ArrayList<>();

    public SummarySymbol(String name, int declarationIndex) {
        this.name = name;
        this.declarationIndex = declarationIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void putElement(SummaryElement element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStartIndex() {
        return declarationIndex;
    }

    @Override
    public int getEndIndex() {
        return declarationIndex;
    }

    @Override
    public String toString() {
        return "Symbol{name=" + name + ",index=" + declarationIndex + "}";
    }

    @Override
    public void updateIndices(Function<Integer, Integer> h) {
        declarationIndex = h.apply(declarationIndex);
    }

    public SummarySymbol addTag(String tag) {
        suggestionTags.add(tag);
        return this;
    }

    public ArrayList<String> getSuggestionTags() {
        return suggestionTags;
    }
}
