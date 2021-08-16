package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.clear.ClearCommand;
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
import static com.energyxxer.prismarine.PrismarineProductions.ofType;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.LINE_GLUE;

public class ClearCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"clear"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("clear"),
                group(
                        productions.getOrCreateStructure("ENTITY"),
                        group(
                                ofType(LINE_GLUE),
                                productions.getOrCreateStructure("ITEM_TAGGED"),
                                TridentProductions.integer(productions).setOptional().setName("AMOUNT").addTags("cspn:Amount")
                        ).setOptional().setName("INNER")
                ).setOptional().setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Item item = (Item) pattern.findThenEvaluate("INNER.INNER.ITEM_TAGGED", null, ctx, NBTMode.TESTING);

        int amount = (int) pattern.findThenEvaluate("INNER.INNER.AMOUNT", -1, ctx);

        try {
            return new ClearCommand((Entity) pattern.findThenEvaluate("INNER.ENTITY", null, ctx), item, amount);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("INNER.ENTITY"))
                    .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.tryFind("INNER.INNER.AMOUNT"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("INNER.INNER.ITEM_TAGGED"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}