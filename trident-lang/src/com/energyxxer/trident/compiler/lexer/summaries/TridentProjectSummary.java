package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.prismarine.summaries.PrismarineProjectSummary;
import com.energyxxer.prismarine.summaries.PrismarineSummaryModule;
import com.energyxxer.prismarine.summaries.SummaryBlock;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.trident.TridentSuiteConfiguration;
import com.energyxxer.trident.compiler.ResourceLocation;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TridentProjectSummary extends PrismarineProjectSummary {
    public static final int PASS_VALIDATION = 0;
    public static final int PASS_DIRECTIVES = 1;
    public static final int PASS_FILE_SYMBOLS = 2;
    public static final int PASS_SET_SYMBOL_TYPES = 3;
    public static final int PASS_CODE_ACTIONS_TYPES = 4;
    public static final int PASS_HIGHLIGHT_TYPE_ERRORS = 5;
    public static final int PASS_MEMBER_SUGGESTION = 6;

    private ArrayList<ResourceLocation> rawFunctions = new ArrayList<>();
    private HashMap<String, Set<ResourceLocation>> types = new HashMap<>();
    private HashMap<String, Set<ResourceLocation>> tags = new HashMap<>();
    private LinkedHashSet<ResourceLocation> soundEvents = new LinkedHashSet<>();
    private Set<String> objectives = new HashSet<>();


    public void store(File file, PrismarineSummaryModule summaryModule) {
        store(file, (TridentSummaryModule) summaryModule);
    }

    public void store(File file, TridentSummaryModule summaryModule) {
        super.store(file, summaryModule);
        for(SummarySymbol objective : summaryModule.getObjectives()) {
            objectives.add(objective.getName());
        }
    }

    public void addTag(String category, ResourceLocation tagLoc) {
        if(!tags.containsKey(category)) tags.put(category, new HashSet<>());
        tags.get(category).add(tagLoc);
    }

    public void addType(String category, ResourceLocation typeLoc) {
        if(!types.containsKey(category)) types.put(category, new HashSet<>());
        types.get(category).add(typeLoc);
    }


    @Override
    public String toString() {
        return "Project Summary:\nFiles: " + fileSummaryMap + " files\nTags: " + tags + "\nObjectives: " + objectives;
    }

    public Collection<String> getObjectives() {
        return objectives;
    }

    public Collection<ResourceLocation> getFunctionResources(boolean filterOutCompileOnly) {
        Stream<PrismarineSummaryModule> stream = fileSummaries.stream();
        if(filterOutCompileOnly) {
            stream = stream.filter(s -> !((TridentSummaryModule)s).isCompileOnly());
        }
        LinkedHashSet<ResourceLocation> returnValue = stream.map(f -> ((TridentSummaryModule) f).getResourceLocation()).collect(Collectors.toCollection(LinkedHashSet::new));
        returnValue.addAll(rawFunctions);
        return returnValue;
    }

    public void addSoundEvent(ResourceLocation loc) {
        soundEvents.add(loc);
    }

    public HashMap<String, Set<ResourceLocation>> getTypes() {
        return types;
    }

    public HashMap<String, Set<ResourceLocation>> getTags() {
        return tags;
    }

    public void addRawFunction(ResourceLocation loc) {
        rawFunctions.add(loc);
    }

    public Collection<ResourceLocation> getSoundEvents() {
        return new ArrayList<>(soundEvents);
    }

    @Override
    public void join(PrismarineProjectSummary other) {
        super.join(other);
        this.types.putAll(((TridentProjectSummary) other).types);
        this.tags.putAll(((TridentProjectSummary) other).tags);
        this.soundEvents.addAll(((TridentProjectSummary) other).soundEvents);
        this.objectives.addAll(((TridentProjectSummary) other).objectives);
    }

    public SummarySymbol getPrimitiveSymbol(String identifier) {
        PrismarineSummaryModule primitivesSummary = getSummaryForLocation(TridentSuiteConfiguration.PRIMITIVES_SUMMARY_PATH);
        SummaryBlock primitivesPackage = primitivesSummary != null ? primitivesSummary.getFileBlock() : null;
        if(primitivesPackage != null) {
            SummaryBlock classBlock = (SummaryBlock) primitivesPackage.getElementByName(identifier);
            if(classBlock != null) {
                return classBlock.getAssociatedSymbol();
            }
        }
        return null;
    }
}
