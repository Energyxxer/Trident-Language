package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.AdvancementArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementArgumentEntry;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCompletionEntry;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCriterionEntry;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCriterionGroupEntry;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class AdvancementArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"advancements"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        TokenPatternMatch advancementArgumentBlock = group(
                TridentProductions.brace("{"),
                list(
                        group(
                                wrapper(productions.getOrCreateStructure("RESOURCE_LOCATION")).setName("ADVANCEMENT_ENTRY_KEY"),
                                TridentProductions.equals(),
                                choice(
                                        wrapper(TridentProductions.rawBoolean(), (v, p, d) -> {
                                            String advancementName = (String) d[1];
                                            return new AdvancementCompletionEntry(advancementName, (boolean) v);
                                        }),
                                        group(
                                                TridentProductions.brace("{"),
                                                list(
                                                        group(
                                                                wrapper(TridentProductions.identifierA(productions)).setName("CRITERION_NAME"),
                                                                TridentProductions.equals(),
                                                                wrapper(TridentProductions.rawBoolean()).addTags(SuggestionTags.ENABLED).setName("CRITERION_ATTAINED")
                                                        ).setEvaluator((p, d) -> {
                                                            ISymbolContext ctx = (ISymbolContext) d[0];
                                                            String criterionName = (String) p.find("CRITERION_NAME").evaluate(ctx);
                                                            boolean attained = (boolean) p.find("CRITERION_ATTAINED").evaluate(ctx);
                                                            return new AdvancementCriterionEntry(criterionName, attained);
                                                        }),
                                                        TridentProductions.comma()
                                                ).setOptional().setName("CRITERION_LIST").setEvaluator((p, d) -> {
                                                    ISymbolContext ctx = (ISymbolContext) d[0];
                                                    AdvancementCriterionGroupEntry group = new AdvancementCriterionGroupEntry((String) d[1]);

                                                    for(TokenPattern<?> rawEntry : ((TokenList) p).getContentsExcludingSeparators()) {
                                                        group.addCriterion((AdvancementCriterionEntry) rawEntry.evaluate(ctx));
                                                    }
                                                    return group;
                                                }),
                                                TridentProductions.brace("}")
                                        ).setName("CRITERION_GROUP").setSimplificationFunction(d -> {
                                            d.pattern = d.pattern.tryFind("CRITERION_LIST");
                                        }).setEvaluator((p, d) -> new AdvancementCriterionGroupEntry((String) d[1]))
                                ).setName("ADVANCEMENT_ENTRY_VALUE")
                        ).setName("ADVANCEMENT_ENTRY").setSimplificationFunction(d -> {
                            ISymbolContext ctx = (ISymbolContext) d.data[0];
                            String advancementName = ((ResourceLocation) d.pattern.find("ADVANCEMENT_ENTRY_KEY").evaluate(ctx)).toString();

                            d.pattern = d.pattern.find("ADVANCEMENT_ENTRY_VALUE");
                            d.data = new Object[] {ctx, advancementName};
                        }),
                        TridentProductions.comma()
                ).setOptional().setName("ADVANCEMENT_LIST").setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    PathContext pathContext = (PathContext) d[1];
                    AdvancementArgument advancements = new AdvancementArgument();

                    for(TokenPattern<?> rawArg : ((TokenList) p).getContentsExcludingSeparators()) {
                        advancements.addEntry((AdvancementArgumentEntry) rawArg.evaluate(ctx, pathContext));
                    }

                    return advancements;
                }),
                TridentProductions.brace("}")
        ).setName("ADVANCEMENT_ARGUMENT_BLOCK").setSimplificationFunction(d -> {
            d.pattern = d.pattern.tryFind("ADVANCEMENT_LIST"); //will not simplify if the list does not exist, and
            // thus will execute the following evaluator
        }).setEvaluator((p, d) -> new AdvancementArgument());

        return group(
                literal("advancements").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                advancementArgumentBlock
        ).setSimplificationFunctionContentIndex(2);
    }

    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx, PathContext pathContext) {
        if(true) {
            throw new UnsupportedOperationException(); //this step is optimized away
        }
        TokenList advancementList = (TokenList) pattern.find("ADVANCEMENT_LIST");

        AdvancementArgument advancements = new AdvancementArgument();

        if(advancementList != null) {
            for(TokenPattern<?> rawArg : advancementList.getContents()) {
                if(rawArg.getName().equals("ADVANCEMENT_ENTRY")) {
                    ResourceLocation advancementLoc = new ResourceLocation(rawArg.find("ADVANCEMENT_ENTRY_KEY").flatten(false));
                    TokenPattern<?> rawValue = ((TokenStructure)rawArg.find("ADVANCEMENT_ENTRY_VALUE")).getContents();
                    switch(rawValue.getName()) {
                        case "BOOLEAN": {
                            advancements.addEntry(new AdvancementCompletionEntry(advancementLoc.toString(), rawValue.flatten(false).equals("true")));
                            break;
                        }
                        case "CRITERION_GROUP": {
                            AdvancementCriterionGroupEntry criteria = parseCriterionGroup(rawValue, ctx, advancementLoc);
                            advancements.addEntry(criteria);
                            break;
                        }
                        default: {
                            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + rawValue.getName() + "'", rawValue, ctx);
                        }
                    }
                }
            }
        }

        return advancements;
    }

    private AdvancementCriterionGroupEntry parseCriterionGroup(TokenPattern<?> pattern, ISymbolContext ctx, ResourceLocation advancementLoc) {
        AdvancementCriterionGroupEntry criteria = new AdvancementCriterionGroupEntry(advancementLoc.toString());
        TokenList criterionList = (TokenList) pattern.find("CRITERION_LIST");
        if(criterionList != null) {
            for(TokenPattern<?> rawArg : criterionList.getContents()) {
                if(rawArg.getName().equals("CRITERION_ENTRY")) {
                    String criterionName = (String) rawArg.find("CRITERION_NAME.IDENTIFIER_A").evaluate(ctx);
                    boolean value = rawArg.find("BOOLEAN").flatten(false).equals("true");
                    criteria.addCriteria(new AdvancementCriterionEntry(criterionName, value));
                }
            }
        }
        return criteria;
    }
}
