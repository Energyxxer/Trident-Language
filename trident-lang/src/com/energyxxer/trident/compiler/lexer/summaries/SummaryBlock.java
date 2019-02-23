package com.energyxxer.trident.compiler.lexer.summaries;

import java.util.ArrayList;
import java.util.function.Function;

public class SummaryBlock implements SummaryElement {
    private boolean fixed = false;
    private int startIndex;
    private int endIndex;
    private ArrayList<SummaryElement> subElements = new ArrayList<>();

    public SummaryBlock() {
        this.fixed = true;
        this.startIndex = 0;
        this.endIndex = Integer.MAX_VALUE;
    }

    public SummaryBlock(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void putElement(SummaryElement element) {
        subElements.add(element);
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

    public void surroundBlock(int start, int end) {
        SummaryBlock sub = null;
        int i = 0;
        for(; i < subElements.size(); i++) {
            SummaryElement elem = subElements.get(i);
            if(elem.getStartIndex() < start) continue;
            if(elem.getStartIndex() >= end) break;
            if(sub == null) sub = new SummaryBlock(start, end);
            sub.putElement(elem);
            subElements.remove(i);
            i--;
        }
        if(sub != null) {
             subElements.add(i, sub);
        }
    }

    void collectSymbolsVisibleAt(int index, ArrayList<SummarySymbol> list) {
        if(startIndex <= index && index <= endIndex) {
            for(SummaryElement elem : subElements) {
                if(elem instanceof SummaryBlock) ((SummaryBlock) elem).collectSymbolsVisibleAt(index, list);
                else if(elem instanceof SummarySymbol && index >= elem.getStartIndex()) {
                    list.removeIf(e -> e.getName().equals(elem.getName()));
                    list.add(((SummarySymbol) elem));
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
}
