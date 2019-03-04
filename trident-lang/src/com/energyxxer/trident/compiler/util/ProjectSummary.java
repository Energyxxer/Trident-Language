package com.energyxxer.trident.compiler.util;

import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.lexer.summaries.SummarySymbol;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectSummary {
    private HashMap<File, TridentSummaryModule> fileSummaries = new HashMap<>();
    private HashMap<String, ArrayList<TridentUtil.ResourceLocation>> types = new HashMap<>();
    private HashMap<String, ArrayList<TridentUtil.ResourceLocation>> tags = new HashMap<>();
    private ArrayList<TridentUtil.ResourceLocation> soundEvents = new ArrayList<>();
    private ArrayList<String> objectives = new ArrayList<>();
    private ArrayList<SummarySymbol> globalSymbols = new ArrayList<>();
    private ArrayList<Todo> todos = new ArrayList<>();

    public void store(File file, TridentSummaryModule summaryModule) {
        fileSummaries.put(file, summaryModule);
        for(SummarySymbol objective : summaryModule.getObjectives()) {
            if(!objectives.contains(objective.getName())) {
                objectives.add(objective.getName());
            }
        }
        this.todos.addAll(summaryModule.getTodos());
        globalSymbols.addAll(summaryModule.getGlobalSymbols());
    }

    public void addTag(String category, TridentUtil.ResourceLocation tagLoc) {
        if(!tags.containsKey(category)) tags.put(category, new ArrayList<>());
        tags.get(category).add(tagLoc);
    }

    public void addType(String category, TridentUtil.ResourceLocation typeLoc) {
        if(!types.containsKey(category)) types.put(category, new ArrayList<>());
        types.get(category).add(typeLoc);
    }

    public TridentSummaryModule getSummaryForLocation(TridentUtil.ResourceLocation loc) {
        for(TridentSummaryModule summaryModule : fileSummaries.values()) {
            if(summaryModule.getFileLocation() != null && summaryModule.getFileLocation().equals(loc)) return summaryModule;
        }
        return null;
    }

    public Collection<SummarySymbol> getGlobalSymbols() {
        return globalSymbols;
    }

    @Override
    public String toString() {
        return "Project Summary:\nFiles: " + fileSummaries + " files\nTags: " + tags + "\nObjectives: " + objectives;
    }

    public Collection<String> getObjectives() {
        return objectives;
    }

    public Collection<TridentUtil.ResourceLocation> getFunctionResources(boolean filterOutCompileOnly) {
        Stream<TridentSummaryModule> stream = fileSummaries.values().stream();
        if(filterOutCompileOnly) {
            stream = stream.filter(s -> !s.isCompileOnly());
        }
        return stream.map(TridentSummaryModule::getFileLocation).collect(Collectors.toList());
    }

    public void addSoundEvent(TridentUtil.ResourceLocation loc) {
        soundEvents.add(loc);
    }

    public HashMap<String, ArrayList<TridentUtil.ResourceLocation>> getTypes() {
        return types;
    }

    public HashMap<String, ArrayList<TridentUtil.ResourceLocation>> getTags() {
        return tags;
    }

    public ArrayList<TridentUtil.ResourceLocation> getSoundEvents() {
        return soundEvents;
    }

    public ArrayList<Todo> getTodos() {
        return todos;
    }

    public TridentSummaryModule getSummaryForFile(File file) {
        return fileSummaries.get(file);
    }

    public TridentUtil.ResourceLocation getLocationForFile(File file) {
        TridentSummaryModule summary = getSummaryForFile(file);
        return summary != null ? summary.getFileLocation() : null;
    }
}
