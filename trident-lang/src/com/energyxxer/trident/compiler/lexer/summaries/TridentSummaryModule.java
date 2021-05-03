package com.energyxxer.trident.compiler.lexer.summaries;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.summaries.CachedSymbolReference;
import com.energyxxer.prismarine.summaries.PrismarineProjectSummary;
import com.energyxxer.prismarine.summaries.PrismarineSummaryModule;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.sets.ValueAccessExpressionSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TridentSummaryModule extends PrismarineSummaryModule {
    public static final BiConsumer<TokenPattern<?>, Lexer> CLEAR_PRE_BLOCK_DECLARATIONS = (p, l) -> {
        if(l.getSummaryModule() != null) {
            ((TridentSummaryModule) l.getSummaryModule()).clearPreBlockDeclarations();
        }
    };
    public static final BiConsumer<TokenPattern<?>, Lexer> CAPTURE_PRE_BLOCK_DECLARATIONS = (p, l) -> {
        if(l.getSummaryModule() != null) {
            for(PreBlockDeclaration declaration : ((TridentSummaryModule) l.getSummaryModule()).getPreBlockDeclarations()) {
                SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), declaration.declarationPattern.flatten(false), TridentSymbolVisibility.LOCAL, p.getStringLocation().index + 1);
                ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(declaration.declarationPattern);
                sym.setDeclarationPattern(declaration.declarationPattern);
                sym.setType(new CachedSymbolReference(fs -> ValueAccessExpressionSet.getTypeSymbolFromConstraint(fs, declaration.constraintsPattern)));
                if(declaration.tags != null) {
                    for(String tag : declaration.tags) {
                        sym.addTag(tag);
                    }
                }
                sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                ((TridentSummaryModule) l.getSummaryModule()).peek().putElement(sym);
            }
            ((TridentSummaryModule) l.getSummaryModule()).clearPreBlockDeclarations();
        }
    };

    private ResourceLocation resourceLocation;
    private ArrayList<SummarySymbol> objectives = new ArrayList<>();
    private ArrayList<ResourceLocation> functionTags = new ArrayList<>();

    private boolean compileOnly = false;
    private boolean directivesLocked = false;

    private final ArrayList<PreBlockDeclaration> preBlockDeclarations = new ArrayList<>();

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

    public void clearPreBlockDeclarations() {
        preBlockDeclarations.clear();
    }

    public List<PreBlockDeclaration> getPreBlockDeclarations() {
        return preBlockDeclarations;
    }


    public PreBlockDeclaration addPreBlockDeclaration(TokenPattern<?> declarationPattern) {
        PreBlockDeclaration declaration = new PreBlockDeclaration(declarationPattern);
        preBlockDeclarations.add(declaration);
        return declaration;
    }

    public PreBlockDeclaration addPreBlockDeclaration(TokenPattern<?> declarationPattern, TokenPattern<?> constraintsPattern) {
        PreBlockDeclaration declaration = new PreBlockDeclaration(declarationPattern, constraintsPattern);
        preBlockDeclarations.add(declaration);
        return declaration;
    }

    public static class PreBlockDeclaration {
        public TokenPattern<?> declarationPattern;
        public TokenPattern<?> constraintsPattern;
        public String[] tags = null;

        public PreBlockDeclaration(TokenPattern<?> declarationPattern) {
            this(declarationPattern, null);
        }

        public PreBlockDeclaration(TokenPattern<?> declarationPattern, TokenPattern<?> constraintsPattern) {
            this.declarationPattern = declarationPattern;
            this.constraintsPattern = constraintsPattern;
        }

        public PreBlockDeclaration setTags(String[] tags) {
            this.tags = tags;
            return this;
        }
    }
}
