package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.trident.compiler.semantics.Symbol;

import java.util.ArrayList;
import java.util.function.Function;

public class SummarySymbol implements SummaryElement {
    private TridentSummaryModule parentSummary;
    private String name;
    private int declarationIndex;
    private Symbol.SymbolVisibility visibility = Symbol.SymbolVisibility.LOCAL;
    private ArrayList<String> suggestionTags = new ArrayList<>();

    public SummarySymbol(TridentSummaryModule parentSummary, String name, int declarationIndex) {
        this.parentSummary = parentSummary;
        this.name = name;
        this.declarationIndex = declarationIndex;
    }

    public Symbol.SymbolVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Symbol.SymbolVisibility visibility) {
        this.visibility = visibility;
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
        return "(" + visibility.toString().toLowerCase() + ") " + name + "@" + declarationIndex;
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

    @Override
    public SummaryModule getParentFileSummary() {
        return parentSummary;
    }
}
