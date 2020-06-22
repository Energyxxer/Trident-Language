package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.semantics.Symbol;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;

public class SummarySymbol implements SummaryElement {
    private TridentSummaryModule parentSummary;
    private String name;
    private int declarationIndex;
    private Symbol.SymbolVisibility visibility = Symbol.SymbolVisibility.LOCAL;
    private ArrayList<String> suggestionTags = new ArrayList<>();
    private SummaryBlock subBlock = null;
    private boolean isMember = false;
    private boolean isStaticField = false;
    private boolean isInstanceField = false;

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
        scopeStart = h.apply(scopeStart);
        scopeEnd = h.apply(scopeEnd);
    }

    @Override
    public void collectSymbolsVisibleAt(int index, ArrayList<SummarySymbol> list, boolean fromSameFile) {
        if(!isMember && (index < 0 || index > getStartIndex()) && (fromSameFile || visibility != Symbol.SymbolVisibility.PRIVATE)) {
            list.removeIf(e -> e.getName().equals(name));
            list.add(this);
        }
    }

    @Override
    public void collectSubSymbolsForPath(String[] path, int pathStart, ArrayList<SummarySymbol> list, TridentUtil.ResourceLocation fromFile, int inFileIndex) {
        if(path.length == pathStart) {
            if(isMember || (isStaticField && (visibility == Symbol.SymbolVisibility.PUBLIC || (visibility == Symbol.SymbolVisibility.LOCAL && Objects.equals(fromFile, parentSummary.getFileLocation())) || (visibility == Symbol.SymbolVisibility.PRIVATE && scopeStart <= inFileIndex && inFileIndex <= scopeEnd)))) {
                list.add(this);
            }
        } else {
            if(subBlock == null) return;
            subBlock.collectSubSymbolsForPath(path, pathStart, list, fromFile, inFileIndex);
        }
    }

    @Override
    public void collectGlobalSymbols(ArrayList<SummarySymbol> list) {
        if(!isMember && getVisibility() == Symbol.SymbolVisibility.GLOBAL) {
            list.removeIf(e -> e.getName().equals(this.getName()));
            list.add(this);
        }
    }

    public SummarySymbol addTag(String tag) {
        suggestionTags.add(tag);
        return this;
    }

    public ArrayList<String> getSuggestionTags() {
        return suggestionTags;
    }

    public boolean hasSubBlock() {
        return subBlock != null;
    }

    public void setSubBlock(SummaryBlock subBlock) {
        this.subBlock = subBlock;
    }

    public boolean isMember() {
        return isMember;
    }

    public void setMember(boolean member) {
        isMember = member;
    }

    public void setStaticField(boolean staticField) {
        isStaticField = staticField;
    }

    public void setInstanceField(boolean instanceField) {
        isInstanceField = instanceField;
    }

    @Override
    public SummaryModule getParentFileSummary() {
        return parentSummary;
    }

    public boolean isField() {
        return isInstanceField || isStaticField;
    }

    private int scopeStart, scopeEnd;

    public void setFieldScope(int start, int end) {
        this.scopeStart = start;
        this.scopeEnd = end;
    }
}
