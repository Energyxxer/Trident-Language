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
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class AdvancementArgumentParser implements PatternSwitchProviderUnit<ISymbolContext> {
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
                                        wrapper(TridentProductions.rawBoolean(), (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            String advancementName = (String) d[0];
                                            return new AdvancementCompletionEntry(advancementName, (boolean) v);
                                        }),
                                        group(
                                                TridentProductions.brace("{"),
                                                list(
                                                        group(
                                                                wrapper(TridentProductions.identifierA(productions)).setName("CRITERION_NAME"),
                                                                TridentProductions.equals(),
                                                                wrapper(TridentProductions.rawBoolean()).addTags(SuggestionTags.ENABLED).setName("CRITERION_ATTAINED")
                                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                                            String criterionName = (String) p.find("CRITERION_NAME").evaluate(ctx, null);
                                                            boolean attained = (boolean) p.find("CRITERION_ATTAINED").evaluate(ctx, null);
                                                            return new AdvancementCriterionEntry(criterionName, attained);
                                                        }),
                                                        TridentProductions.comma()
                                                ).setOptional().setName("CRITERION_LIST").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                                    AdvancementCriterionGroupEntry group = new AdvancementCriterionGroupEntry((String) d[0]);

                                                    for(TokenPattern<?> rawEntry : ((TokenList) p).getContentsExcludingSeparators()) {
                                                        group.addCriterion((AdvancementCriterionEntry) rawEntry.evaluate(ctx, null));
                                                    }
                                                    return group;
                                                }),
                                                TridentProductions.brace("}")
                                        ).setName("CRITERION_GROUP").setSimplificationFunction(d -> {
                                            d.pattern = d.pattern.tryFind("CRITERION_LIST");
                                        }).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new AdvancementCriterionGroupEntry((String) d[0]))
                                ).setName("ADVANCEMENT_ENTRY_VALUE")
                        ).setName("ADVANCEMENT_ENTRY").setSimplificationFunction(d -> {
                            ISymbolContext ctx = (ISymbolContext) d.ctx;
                            TokenPattern<?> pattern = d.pattern;
                            d.unlock(); d = null;

                            String advancementName = ((ResourceLocation) pattern.find("ADVANCEMENT_ENTRY_KEY").evaluate(ctx, null)).toString();

                            TokenPattern.SimplificationDomain.get(pattern.find("ADVANCEMENT_ENTRY_VALUE"), ctx, new Object[] {advancementName});
                        }),
                        TridentProductions.comma()
                ).setOptional().setName("ADVANCEMENT_LIST").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    AdvancementArgument advancements = new AdvancementArgument();

                    for(TokenPattern<?> rawArg : ((TokenList) p).getContentsExcludingSeparators()) {
                        advancements.addEntry((AdvancementArgumentEntry) rawArg.evaluate(ctx, null));
                    }

                    return advancements;
                }),
                TridentProductions.brace("}")
        ).setName("ADVANCEMENT_ARGUMENT_BLOCK").setSimplificationFunction(d -> {
            d.pattern = d.pattern.tryFind("ADVANCEMENT_LIST"); //will not simplify if the list does not exist, and
            // thus will execute the following evaluator
        }).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new AdvancementArgument());

        return group(
                literal("advancements").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                advancementArgumentBlock
        ).setSimplificationFunctionContentIndex(2);
    }

    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }

    private AdvancementCriterionGroupEntry parseCriterionGroup(TokenPattern<?> pattern, ISymbolContext ctx, ResourceLocation advancementLoc) {
        AdvancementCriterionGroupEntry criteria = new AdvancementCriterionGroupEntry(advancementLoc.toString());
        TokenList criterionList = (TokenList) pattern.find("CRITERION_LIST");
        if(criterionList != null) {
            for(TokenPattern<?> rawArg : criterionList.getContents()) {
                if(rawArg.getName().equals("CRITERION_ENTRY")) {
                    String criterionName = (String) rawArg.find("CRITERION_NAME.IDENTIFIER_A").evaluate(ctx, null);
                    boolean value = rawArg.find("BOOLEAN").flatten(false).equals("true");
                    criteria.addCriteria(new AdvancementCriterionEntry(criterionName, value));
                }
            }
        }
        return criteria;
    }
}
