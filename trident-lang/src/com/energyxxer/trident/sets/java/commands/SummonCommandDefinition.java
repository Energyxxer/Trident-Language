package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.sets.trident.TridentLiteralSet;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class SummonCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"summon"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("summon"),
                productions.getOrCreateStructure("NEW_ENTITY_LITERAL"),
                optional(
                        productions.getOrCreateStructure("COORDINATE_SET"),
                        wrapperOptional(productions.getOrCreateStructure("NBT_COMPOUND")).addTags("cspn:NBT").setName("SUMMON_NBT")
                ).setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TridentLiteralSet.SummonData data = (TridentLiteralSet.SummonData) pattern.find("NEW_ENTITY_LITERAL").evaluate(ctx, null);
        data.pos = (CoordinateSet) pattern.findThenEvaluate("INNER.COORDINATE_SET", null, ctx, null);
        data.mergeNBT((TagCompound) pattern.findThenEvaluate("INNER.SUMMON_NBT", null, ctx, null));
        data.analyzeNBT(pattern, ctx);

        try {
            return data.constructSummon();
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("NEW_ENTITY_LITERAL"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

}