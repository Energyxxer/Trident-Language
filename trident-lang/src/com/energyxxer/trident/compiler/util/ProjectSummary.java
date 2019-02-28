package com.energyxxer.trident.compiler.util;

import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.lexer.summaries.SummarySymbol;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class ProjectSummary {
    private HashMap<File, TridentSummaryModule> fileSummaries = new HashMap<>();
    private HashMap<String, ArrayList<String>> tags = new HashMap<>();
    private ArrayList<String> objectives = new ArrayList<>();
    private ArrayList<SummarySymbol> globalSymbols = new ArrayList<>();

    public void store(File file, TridentSummaryModule summaryModule) {
        fileSummaries.put(file, summaryModule);
        for(SummarySymbol objective : summaryModule.getObjectives()) {
            if(!objectives.contains(objective.getName())) {
                objectives.add(objective.getName());
            }
        }
        globalSymbols.addAll(summaryModule.getGlobalSymbols());
    }

    public void addTag(String category, TridentUtil.ResourceLocation tagLoc) {
        if(!tags.containsKey(category)) tags.put(category, new ArrayList<>());
        tags.get(category).add(tagLoc.toString());
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
}
