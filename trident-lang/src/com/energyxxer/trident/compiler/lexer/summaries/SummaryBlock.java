package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.trident.compiler.semantics.Symbol;

import java.util.ArrayList;
import java.util.function.Function;

public class SummaryBlock implements SummaryElement {
    private boolean fixed = false;
    private TridentSummaryModule parentSummary;
    private int startIndex;
    private int endIndex;
    private ArrayList<SummaryElement> subElements = new ArrayList<>();

    public SummaryBlock(TridentSummaryModule parentSummary) {
        this(parentSummary, 0, Integer.MAX_VALUE);
        this.fixed = true;
    }

    public SummaryBlock(TridentSummaryModule parentSummary, int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.parentSummary = parentSummary;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void putElement(SummaryElement element) {
        clearEmptyBlocks();
        int i = subElements.size();
        while(i > 0) {
            if(element.getStartIndex() >= subElements.get(i-1).getStartIndex()) break;
            i--;
        }
        subElements.add(i, element);
    }

    @Override
    public String toString() {
        return subElements.toString();
    }

    @Override
    public int getStartIndex() {
        return startIndex;
    }

    @Override
    public int getEndIndex() {
        return endIndex;
    }

    void clearEmptyBlocks() {
        if(!subElements.isEmpty()) {
            SummaryElement last = subElements.get(subElements.size()-1);
            if(last instanceof SummaryBlock && ((SummaryBlock) last).isEmpty()) {
                subElements.remove(last);
            }
        }
    }

    public void surroundBlock(int start, int end) {
        clearEmptyBlocks();
        SummaryBlock sub = null;
        int i = 0;
        for(; i < subElements.size(); i++) {
            SummaryElement elem = subElements.get(i);
            if(elem.getStartIndex() < start) continue;
            if(elem.getStartIndex() >= end) break;
            if(sub == null) sub = new SummaryBlock(parentSummary, start, end);
            sub.putElement(elem);
            subElements.remove(i);
            i--;
        }
        if(sub == null) sub = new SummaryBlock(parentSummary, start, end);
        subElements.add(i, sub);
    }

    public void putLateElement(SummaryElement elem) {
        boolean inserted = false;
        for(SummaryElement sub : subElements) {
            if (sub instanceof SummaryBlock && sub.getStartIndex() <= elem.getStartIndex() && elem.getEndIndex() <= sub.getEndIndex()) {
                sub.putElement(elem);
                inserted = true;
                break;
            }
        }
        if(inserted) subElements.remove(elem);
    }

    void collectSymbolsVisibleAt(int index, ArrayList<SummarySymbol> list, boolean fromSameFile) {
        if(subElements.isEmpty()) return;
        if(index < 0 || startIndex <= index && index <= endIndex) {
            for(SummaryElement elem : subElements) {
                if(elem instanceof SummaryBlock && index >= 0) ((SummaryBlock) elem).collectSymbolsVisibleAt(index, list, fromSameFile);
                else if(elem instanceof SummarySymbol && (index < 0 || index >= elem.getStartIndex())) {
                    if(fromSameFile || ((SummarySymbol) elem).getVisibility() != Symbol.SymbolVisibility.PRIVATE) {
                        list.removeIf(e -> e.getName().equals(elem.getName()));
                        list.add(((SummarySymbol) elem));
                    }
                }
            }
        }
    }

    public void collectGlobalSymbols(ArrayList<SummarySymbol> list) {
        for(SummaryElement elem : subElements) {
            if(elem instanceof SummaryBlock) ((SummaryBlock) elem).collectGlobalSymbols(list);
            else if(elem instanceof SummarySymbol) {
                if(((SummarySymbol) elem).getVisibility() == Symbol.SymbolVisibility.GLOBAL) {
                    list.removeIf(e -> e.getName().equals(elem.getName()));
                    list.add((SummarySymbol) elem);
                }
            }
        }
    }

    @Override
    public void updateIndices(Function<Integer, Integer> h) {
        if(!fixed) {
            startIndex = h.apply(startIndex);
            endIndex = h.apply(endIndex);
        }

        for(SummaryElement elem : subElements) {
            elem.updateIndices(h);
        }
    }

    public boolean isEmpty() {
        return subElements.isEmpty();
    }

    @Override
    public SummaryModule getParentFileSummary() {
        return parentSummary;
    }
}
