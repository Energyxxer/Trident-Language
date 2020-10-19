package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.prismarine.summaries.PrismarineProjectSummary;
import com.energyxxer.prismarine.summaries.PrismarineSummaryModule;
import com.energyxxer.prismarine.summaries.SummarySymbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TridentSummaryModule extends PrismarineSummaryModule {
    private ResourceLocation resourceLocation;
    private ArrayList<SummarySymbol> objectives = new ArrayList<>();
    private ArrayList<ResourceLocation> functionTags = new ArrayList<>();

    private boolean compileOnly = false;
    private boolean directivesLocked = false;

    public TridentSummaryModule() {
        this(null);
    }

    public TridentSummaryModule(TridentProjectSummary parentSummary) {
        super(parentSummary);
    }

    public void addObjective(SummarySymbol sym) {
        objectives.add(sym);
    }

    public Collection<SummarySymbol> getObjectives() {
        return objectives;
    }

    public Collection<String> getAllObjectives() {
        ArrayList<String> objectives = new ArrayList<>();
        if(parentSummary != null) {
            objectives.addAll(((TridentProjectSummary) parentSummary).getObjectives());
        }
        for(SummarySymbol obj : this.objectives) {
            objectives.remove(obj.getName());
            objectives.add(obj.getName());
        }
        return objectives;
    }

    public void addFunctionTag(ResourceLocation loc) {
        if(!directivesLocked) functionTags.add(loc);
    }

    public List<ResourceLocation> getFunctionTags() {
        return functionTags;
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

    public TridentProjectSummary getParentSummary() {
        return (TridentProjectSummary) parentSummary;
    }

    public void setParentSummary(PrismarineProjectSummary parentSummary) {
        this.parentSummary = (TridentProjectSummary) parentSummary;
    }

    @Override
    public String toString() {
        return "File Summary for " + fileLocation + ": \n" +
                "    Resource Location: " + resourceLocation + "\n" +
                "    Requires: " + requires + "\n" +
                "    Function Tags: " + functionTags + "\n" +
                "    Objectives: " + objectives.stream().map(SummarySymbol::getName).collect(Collectors.joining(", ")) + "\n" +
                "    Scopes: " + fileBlock.toString() + "\n";
    }

    public void setResourceLocation(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }
}
