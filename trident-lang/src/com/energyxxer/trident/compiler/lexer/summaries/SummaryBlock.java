package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.semantics.Symbol;

import java.util.ArrayList;
import java.util.function.Function;

public class SummaryBlock implements SummaryElement {
    private boolean fixed = false;
    private SummarySymbol associatedSymbol = null;
    private TridentSummaryModule parentSummary;
    private int startIndex;
    private int endIndex;
    private ArrayList<SummaryElement> subElements = new ArrayList<>();

    public SummaryBlock(TridentSummaryModule parentSummary) {
        this(parentSummary, 0, Integer.MAX_VALUE);
        this.fixed = true;
    }

    public SummaryBlock(TridentSummaryModule parentSummary, int startIndex, int endIndex) {
        this(parentSummary, startIndex, endIndex, null);
    }

    public SummaryBlock(TridentSummaryModule parentSummary, int startIndex, int endIndex, SummarySymbol associatedSymbol) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.parentSummary = parentSummary;
        this.associatedSymbol = associatedSymbol;
        if(associatedSymbol != null) associatedSymbol.setSubBlock(this);
    }

    @Override
    public String getName() {
        return associatedSymbol != null ? associatedSymbol.getName() : null;
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
        return (associatedSymbol != null ? getName() + ": " : "") + subElements.toString();
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
            if(last instanceof SummaryBlock && ((SummaryBlock) last).isEmpty() && ((SummaryBlock) last).associatedSymbol == null) {
                subElements.remove(last);
            }
        }
    }

    public void surroundBlock(int start, int end) {
        surroundBlock(start, end, null);
    }

    public void surroundBlock(int start, int end, SummarySymbol associatedSymbol) {
        clearEmptyBlocks();
        SummaryBlock sub = null;
        int i = 0;
        for(; i < subElements.size(); i++) {
            SummaryElement elem = subElements.get(i);
            if(elem.getStartIndex() < start) continue;
            if(elem.getStartIndex() >= end) break;
            if(elem instanceof SummarySymbol && ((SummarySymbol) elem).isField()) {
                ((SummarySymbol) elem).setFieldScope(start, end);
            }
            if(sub == null) sub = new SummaryBlock(parentSummary, start, end, associatedSymbol);
            sub.putElement(elem);
            subElements.remove(i);
            i--;
        }
        if(sub == null) sub = new SummaryBlock(parentSummary, start, end, associatedSymbol);
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

    @Override
    public void collectSymbolsVisibleAt(int index, ArrayList<SummarySymbol> list, boolean fromSameFile) {
        if(associatedSymbol != null) associatedSymbol.collectSymbolsVisibleAt(index, list, fromSameFile);
        if(subElements.isEmpty()) return;
        if((index < 0 && fixed) || (startIndex <= index && index <= endIndex)) {
            for(SummaryElement elem : subElements) {
                elem.collectSymbolsVisibleAt(index, list, fromSameFile);
            }
        }
    }

    @Override
    public void collectSubSymbolsForPath(String[] path, int pathStart, ArrayList<SummarySymbol> list, TridentUtil.ResourceLocation fromFile, int inFileIndex) {
        if(pathStart == path.length) {
            if(associatedSymbol != null) {
                list.add(associatedSymbol);
            }
        } else if(pathStart == path.length-1) {
            for(SummaryElement elem : subElements) {
                elem.collectSubSymbolsForPath(path, path.length, list, fromFile, inFileIndex);
            }
        } else {
            for(SummaryElement elem : subElements) {
                if(path[pathStart+1].equals(elem.getName())) {
                    elem.collectSubSymbolsForPath(path, pathStart+1, list, fromFile, inFileIndex);
                }
            }
        }
    }

    @Override
    public void collectGlobalSymbols(ArrayList<SummarySymbol> list) {
        if(associatedSymbol != null) associatedSymbol.collectGlobalSymbols(list);
        for(SummaryElement elem : subElements) {
            elem.collectGlobalSymbols(list);
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

    @Override
    public Symbol.SymbolVisibility getVisibility() {
        return associatedSymbol != null ? associatedSymbol.getVisibility() : Symbol.SymbolVisibility.LOCAL;
    }
}
