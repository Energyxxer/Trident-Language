package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.setblock.SetblockCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "setblock")
public class SetblockParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        CoordinateSet pos = CoordinateParser.parse(pattern.find("COORDINATE_SET"), ctx);
        Block block = CommonParsers.parseBlock(pattern.find("BLOCK"), ctx);
        SetblockCommand.OldBlockHandlingMode mode = SetblockCommand.OldBlockHandlingMode.DEFAULT;

        TokenPattern<?> rawMode = pattern.find("OLD_BLOCK_HANDLING");
        if(rawMode != null) {
            mode = SetblockCommand.OldBlockHandlingMode.valueOf(rawMode.flatten(false).toUpperCase());
        }

        try {
            return new SetblockCommand(pos, block, mode);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("BLOCK"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", rawMode, ctx);
        }
    }
}
