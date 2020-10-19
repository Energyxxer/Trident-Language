package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.setblock.SetblockCommand;
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

public class SetblockCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"setblock"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("setblock"),
                productions.getOrCreateStructure("COORDINATE_SET"),
                productions.getOrCreateStructure("BLOCK"),
                enumChoice(SetblockCommand.OldBlockHandlingMode.class).setOptional().setName("OLD_BLOCK_HANDLING")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        CoordinateSet pos = (CoordinateSet) pattern.find("COORDINATE_SET").evaluate(ctx);
        Block block = (Block) pattern.find("BLOCK").evaluate(ctx);
        SetblockCommand.OldBlockHandlingMode mode = (SetblockCommand.OldBlockHandlingMode) pattern.findThenEvaluate("OLD_BLOCK_HANDLING", SetblockCommand.OldBlockHandlingMode.DEFAULT);

        try {
            return new SetblockCommand(pos, block, mode);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("BLOCK"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
