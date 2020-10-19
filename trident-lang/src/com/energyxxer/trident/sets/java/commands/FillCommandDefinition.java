package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.fill.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class FillCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"fill"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("fill"),
                group(productions.getOrCreateStructure("COORDINATE_SET")).setName("FROM").addTags("cspn:From"),
                group(productions.getOrCreateStructure("COORDINATE_SET")).setName("TO").addTags("cspn:To"),
                productions.getOrCreateStructure("BLOCK"),
                choice(
                        literal("destroy").setEvaluator((p, d) -> new FillDestroyMode()),
                        literal("hollow").setEvaluator((p, d) -> new FillHollowMode()),
                        literal("keep").setEvaluator((p, d) -> new FillKeepMode()),
                        literal("outline").setEvaluator((p, d) -> new FillOutlineMode()),
                        group(literal("replace"), wrapperOptional(productions.getOrCreateStructure("BLOCK_TAGGED")).setName("BLOCK_TO_REPLACE")).setEvaluator((p, d) -> new FillReplaceMode((Block) p.findThenEvaluate("BLOCK_TO_REPLACE", null, (ISymbolContext) d[0])))
                ).setOptional().setName("FILL_MODE")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        CoordinateSet from = (CoordinateSet) pattern.find("FROM.COORDINATE_SET").evaluate(ctx);
        CoordinateSet to = (CoordinateSet) pattern.find("TO.COORDINATE_SET").evaluate(ctx);

        Block block = (Block) pattern.find("BLOCK").evaluate(ctx);
        FillCommand.FillMode mode = (FillCommand.FillMode) pattern.findThenEvaluateLazyDefault("FILL_MODE", FillReplaceMode::new, ctx);

        try {
            return new FillCommand(from, to, block, mode);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("BLOCK"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
