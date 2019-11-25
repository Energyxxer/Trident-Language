package com.energyxxer.trident.compiler.util;

import com.energyxxer.enxlex.lexical_analysis.summary.ProjectSummary;
import com.energyxxer.enxlex.lexical_analysis.summary.Todo;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.lexer.summaries.SummarySymbol;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TridentProjectSummary implements ProjectSummary {
    private ArrayList<TridentSummaryModule> fileSummaries = new ArrayList<>();
    private ArrayList<TridentUtil.ResourceLocation> rawFunctions = new ArrayList<>();
    private HashMap<File, TridentSummaryModule> fileSummaryMap = new HashMap<>();
    private HashMap<String, Set<TridentUtil.ResourceLocation>> types = new HashMap<>();
    private HashMap<String, Set<TridentUtil.ResourceLocation>> tags = new HashMap<>();
    private LinkedHashSet<TridentUtil.ResourceLocation> soundEvents = new LinkedHashSet<>();
    private Set<String> objectives = new HashSet<>();
    private Set<SummarySymbol> globalSymbols = new LinkedHashSet<>();
    private Set<Todo> todos = new HashSet<>();

    public void store(File file, TridentSummaryModule summaryModule) {
        if(file != null) fileSummaryMap.put(file, summaryModule);
        fileSummaries.add(summaryModule);
        for(SummarySymbol objective : summaryModule.getObjectives()) {
            objectives.add(objective.getName());
        }
        this.todos.addAll(summaryModule.getTodos());
        globalSymbols.addAll(summaryModule.getGlobalSymbols());
    }

    public void addTag(String category, TridentUtil.ResourceLocation tagLoc) {
        if(!tags.containsKey(category)) tags.put(category, new HashSet<>());
        tags.get(category).add(tagLoc);
    }

    public void addType(String category, TridentUtil.ResourceLocation typeLoc) {
        if(!types.containsKey(category)) types.put(category, new HashSet<>());
        types.get(category).add(typeLoc);
    }

    public TridentSummaryModule getSummaryForLocation(TridentUtil.ResourceLocation loc) {
        for(TridentSummaryModule summaryModule : fileSummaries) {
            if(summaryModule.getFileLocation() != null && summaryModule.getFileLocation().equals(loc)) return summaryModule;
        }
        return null;
    }

    public Collection<SummarySymbol> getGlobalSymbols() {
        return globalSymbols;
    }

    @Override
    public String toString() {
        return "Project Summary:\nFiles: " + fileSummaryMap + " files\nTags: " + tags + "\nObjectives: " + objectives;
    }

    public Collection<String> getObjectives() {
        return objectives;
    }

    public Collection<TridentUtil.ResourceLocation> getFunctionResources(boolean filterOutCompileOnly) {
        Stream<TridentSummaryModule> stream = fileSummaries.stream();
        if(filterOutCompileOnly) {
            stream = stream.filter(s -> !s.isCompileOnly());
        }
        LinkedHashSet<TridentUtil.ResourceLocation> returnValue = stream.map(TridentSummaryModule::getFileLocation).collect(Collectors.toCollection(LinkedHashSet::new));
        returnValue.addAll(rawFunctions);
        return returnValue;
    }

    public void addSoundEvent(TridentUtil.ResourceLocation loc) {
        soundEvents.add(loc);
    }

    public HashMap<String, Set<TridentUtil.ResourceLocation>> getTypes() {
        return types;
    }

    public HashMap<String, Set<TridentUtil.ResourceLocation>> getTags() {
        return tags;
    }

    public void addRawFunction(TridentUtil.ResourceLocation loc) {
        rawFunctions.add(loc);
    }

    public Collection<TridentUtil.ResourceLocation> getSoundEvents() {
        return new ArrayList<>(soundEvents);
    }

    public Collection<Todo> getTodos() {
        return todos;
    }

    public TridentSummaryModule getSummaryForFile(File file) {
        return fileSummaryMap.get(file);
    }

    public TridentUtil.ResourceLocation getLocationForFile(File file) {
        TridentSummaryModule summary = getSummaryForFile(file);
        return summary != null ? summary.getFileLocation() : null;
    }

    public void join(TridentProjectSummary other) {
        this.types.putAll(other.types);
        this.tags.putAll(other.tags);
        this.soundEvents.addAll(other.soundEvents);
        this.objectives.addAll(other.objectives);
        this.globalSymbols.addAll(other.globalSymbols);
    }
}
