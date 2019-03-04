package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.util.ProjectSummary;
import com.energyxxer.trident.compiler.util.Todo;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TridentSummaryModule extends SummaryModule {
    private ProjectSummary parentSummary;
    private TridentUtil.ResourceLocation fileLocation = null;
    private SummaryBlock fileBlock = new SummaryBlock(this);
    private ArrayList<SummarySymbol> objectives = new ArrayList<>();
    private ArrayList<TridentUtil.ResourceLocation> requires = new ArrayList<>();
    private ArrayList<TridentUtil.ResourceLocation> functionTags = new ArrayList<>();
    private ArrayList<Todo> todos = new ArrayList<>();
    private boolean compileOnly = false;
    private boolean directivesLocked = false;

    private boolean searchingSymbols = false;

    private Stack<SummaryBlock> contextStack = new Stack<>();

    public TridentSummaryModule() {
        this(null);
    }

    public TridentSummaryModule(ProjectSummary parentSummary) {
        this.parentSummary = parentSummary;
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

    public SummaryBlock peek() {
        return contextStack.peek();
    }

    public Collection<SummarySymbol> getObjectives() {
        return objectives;
    }

    public Collection<String> getAllObjectives() {
        ArrayList<String> objectives = new ArrayList<>();
        if(parentSummary != null) {
            objectives.addAll(parentSummary.getObjectives());
        }
        for(SummarySymbol obj : this.objectives) {
            objectives.remove(obj.getName());
            objectives.add(obj.getName());
        }
        return objectives;
    }

    public void addRequires(TridentUtil.ResourceLocation loc) {
        if(!directivesLocked) requires.add(loc);
    }

    public void addFunctionTag(TridentUtil.ResourceLocation loc) {
        if(!directivesLocked) functionTags.add(loc);
    }

    public List<TridentUtil.ResourceLocation> getRequires() {
        return requires;
    }

    public List<TridentUtil.ResourceLocation> getFunctionTags() {
        return functionTags;
    }

    public ArrayList<Todo> getTodos() {
        return todos;
    }

    public Collection<SummarySymbol> getSymbolsVisibleAt(int index) {
        ArrayList<SummarySymbol> list = new ArrayList<>();
        if(parentSummary != null) {
            for(SummarySymbol globalSymbol : parentSummary.getGlobalSymbols()) {
                if(!Objects.equals(((TridentSummaryModule) globalSymbol.getParentFileSummary()).getFileLocation(), this.getFileLocation())) {
                    list.add(globalSymbol);
                }
            }
        }
        collectSymbolsVisibleAt(index, list, true);
        return list;
    }

    public void collectSymbolsVisibleAt(int index, ArrayList<SummarySymbol> list, boolean fromSameFile) {
        if(searchingSymbols) return;
        searchingSymbols = true;
        if(parentSummary != null) {
            for(TridentUtil.ResourceLocation required : requires) {
                TridentSummaryModule superFile = parentSummary.getSummaryForLocation(required);
                if(superFile != null) {
                    superFile.collectSymbolsVisibleAt(-1, list, false);
                }
            }
        }
        fileBlock.collectSymbolsVisibleAt(index, list, fromSameFile);
        searchingSymbols = false;
    }

    public Collection<SummarySymbol> getGlobalSymbols() {
        ArrayList<SummarySymbol> list = new ArrayList<>();
        collectGlobalSymbols(list);
        return list;
    }

    public void collectGlobalSymbols(ArrayList<SummarySymbol> list) {
        fileBlock.collectGlobalSymbols(list);
    }

    public void updateIndices(Function<Integer, Integer> h) {
        fileBlock.updateIndices(h);
    }

    public void lockDirectives() {
        directivesLocked = true;
    }

    public void setCompileOnly() {
        if(!directivesLocked) this.compileOnly = true;
    }

    public boolean isCompileOnly() {
        return compileOnly;
    }

    public void setFileLocation(TridentUtil.ResourceLocation location) {
        this.fileLocation = location;
    }

    public TridentUtil.ResourceLocation getFileLocation() {
        return fileLocation;
    }

    public ProjectSummary getParentSummary() {
        return parentSummary;
    }

    public void setParentSummary(ProjectSummary parentSummary) {
        this.parentSummary = parentSummary;
    }

    @Override
    public void onEnd() {
        super.onEnd();
        fileBlock.clearEmptyBlocks();
    }

    @Override
    public String toString() {
        return "File Summary for " + fileLocation + ": \n" +
                "    Requires: " + requires + "\n" +
                "    Function Tags: " + functionTags + "\n" +
                "    Objectives: " + objectives.stream().map(SummarySymbol::getName).collect(Collectors.joining(", ")) + "\n" +
                "    Scopes: " + fileBlock.toString() + "\n";
    }

    public void addTodo(Token token, String text) {
        todos.add(new Todo(token, text));
    }
}
