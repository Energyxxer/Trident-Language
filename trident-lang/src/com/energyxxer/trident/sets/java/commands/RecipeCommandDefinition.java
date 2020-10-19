package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.recipe.RecipeCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.trident.compiler.TridentProductions.symbol;
import static com.energyxxer.prismarine.PrismarineProductions.*;

public class RecipeCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"recipe"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("recipe"),
                enumChoice(RecipeCommand.Action.class).setName("ACTION"),
                productions.getOrCreateStructure("ENTITY"),
                choice(
                        symbol("*").setEvaluator((p, d) -> "*"),
                        group(productions.getOrCreateStructure("RESOURCE_LOCATION")).setEvaluator((p, d) -> p.find("RESOURCE_LOCATION").evaluate((ISymbolContext) d[0]).toString())
                ).addTags("cspn:Recipe").setName("RECIPE")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        RecipeCommand.Action action = (RecipeCommand.Action) pattern.find("ACTION").evaluate();
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx);
        String recipe = (String) pattern.find("RECIPE").evaluate(ctx);
        try {
            return new RecipeCommand(action, entity, recipe);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("ENTITY"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
