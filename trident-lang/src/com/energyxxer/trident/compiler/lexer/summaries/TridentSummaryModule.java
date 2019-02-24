package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;
import java.util.function.Function;

public class TridentSummaryModule extends SummaryModule {
    private SummaryBlock fileBlock = new SummaryBlock();

    private ArrayList<SummarySymbol> objectives = new ArrayList<>();

    private Stack<SummaryBlock> contextStack = new Stack<>();

    public TridentSummaryModule() {
        contextStack.push(fileBlock);
    }

    public void addElement(SummaryElement element) {
        contextStack.peek().putElement(element);
    }

    public void push(SummaryBlock block) {
        contextStack.push(block);
    }

    public void addObjective(SummarySymbol sym) {
        objectives.add(sym);
    }

    public SummaryBlock pop() {
        return contextStack.pop();
    }

    @Override
    public String toString() {
        return fileBlock.toString();
    }

    public SummaryBlock peek() {
        return contextStack.peek();
    }

    public Collection<SummarySymbol> getObjectives() {
        return objectives;
    }

    public Collection<SummarySymbol> getSymbolsVisibleAt(int index) {
        ArrayList<SummarySymbol> list = new ArrayList<>();
        collectSymbolsVisibleAt(index, list);
        return list;
    }

    public void collectSymbolsVisibleAt(int index, ArrayList<SummarySymbol> list) {
        fileBlock.collectSymbolsVisibleAt(index, list);
    }

    public void updateIndices(Function<Integer, Integer> h) {
        fileBlock.updateIndices(h);
    }
}
