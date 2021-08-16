package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.give.GiveCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.trident.compiler.TridentProductions.commandHeader;

public class GiveCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"give"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                commandHeader("give"),
                productions.getOrCreateStructure("ENTITY"),
                productions.getOrCreateStructure("ITEM"),
                TridentProductions.integer(productions).setOptional().setName("AMOUNT").addTags("cspn:Amount")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx);

        Item item = (Item) pattern.find("ITEM").evaluate(ctx, NBTMode.SETTING);
        int amount = (int) pattern.findThenEvaluate("AMOUNT", 1, ctx);

        try {
            return new GiveCommand(entity, item, amount);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("ENTITY"))
                    .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.tryFind("AMOUNT"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("ITEM"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
