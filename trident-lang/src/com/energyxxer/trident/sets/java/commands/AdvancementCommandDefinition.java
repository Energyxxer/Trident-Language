package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.advancement.AdvancementCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import java.util.ArrayList;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class AdvancementCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"advancement"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("advancement"),
                choice(
                        literal("grant").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> AdvancementCommand.Action.GRANT),
                        literal("revoke").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> AdvancementCommand.Action.REVOKE)
                ).setName("ACTION"),
                productions.getOrCreateStructure("ENTITY"),
                choice(
                        literal("everything").setEvaluator(AdvancementCommandDefinition::everythingBranch),
                        group(
                                enumChoice(
                                        AdvancementCommand.Limit.THROUGH,
                                        AdvancementCommand.Limit.FROM,
                                        AdvancementCommand.Limit.UNTIL
                                ).setName("LIMIT"),
                                TridentProductions.noToken().addTags("cspn:Advancement"),
                                productions.getOrCreateStructure("RESOURCE_LOCATION")
                        ).setEvaluator(AdvancementCommandDefinition::fromToUntilBranch),
                        group(
                                literal("only"),
                                TridentProductions.noToken().addTags("cspn:Advancement"),
                                productions.getOrCreateStructure("RESOURCE_LOCATION"),
                                group(
                                        TridentProductions.sameLine(),
                                        list(
                                                TridentProductions.identifierC().addTags("cspn:Criterion"),
                                                TridentProductions.sameLine()
                                        ).setName("CRITERIA_LIST").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            ArrayList<String> criteria = new ArrayList<>();
                                            for (TokenPattern<?> criterion : ((TokenList) p).getContentsExcludingSeparators()) {
                                                criteria.add(criterion.flatten(false));
                                            }
                                            return criteria;
                                        })
                                ).setSimplificationFunctionContentIndex(1).setOptional().setName("CRITERIA")
                        ).setEvaluator(AdvancementCommandDefinition::onlyBranch)
                ).setName("INNER")
        );
    }

    private static Command everythingBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        return new AdvancementCommand((AdvancementCommand.Action) data[0], (Entity) data[1], AdvancementCommand.Limit.EVERYTHING);
    }

    private static Command fromToUntilBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        return new AdvancementCommand((AdvancementCommand.Action) data[0], (Entity) data[1], (AdvancementCommand.Limit) pattern.find("LIMIT").evaluate(ctx, null), ((ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx, null)).toString());
    }

    private static Command onlyBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        ResourceLocation advancement = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx, null);
        ArrayList<String> criteria = (ArrayList<String>) pattern.findThenEvaluate("CRITERIA", null, ctx, null);
        if(criteria == null) criteria = new ArrayList<>();

        return new AdvancementCommand((AdvancementCommand.Action) data[0], (Entity) data[1], AdvancementCommand.Limit.ONLY, advancement.toString(), criteria);
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        AdvancementCommand.Action action = (AdvancementCommand.Action) pattern.find("ACTION").evaluate(ctx, null);
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx, null);

        try {
            return (Command) pattern.find("INNER").evaluate(ctx, new Object[] {action, entity});
        } catch(CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("ENTITY"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
