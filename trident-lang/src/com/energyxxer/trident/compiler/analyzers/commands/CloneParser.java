package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneCommand;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneFilteredCommand;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneMaskedCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "clone")
public class CloneParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        CoordinateSet from = CoordinateParser.parse(pattern.find("FROM.COORDINATE_SET"), ctx);
        CoordinateSet to = CoordinateParser.parse(pattern.find("TO.COORDINATE_SET"), ctx);
        CoordinateSet destination = CoordinateParser.parse(pattern.find("DESTINATION.COORDINATE_SET"), ctx);

        TokenPattern<?> inner = pattern.find("CHOICE");
        if(inner != null) {
            inner = ((TokenStructure)inner).getContents();
            switch(inner.getName()) {
                case "FILTERED": {
                    Block block = CommonParsers.parseBlock(inner.find("BLOCK_TAGGED"), ctx);
                    return new CloneFilteredCommand(from, to, destination, block, parseMode(inner.find("CLONE_MODE")));
                }
                case "MASKED": {
                    return new CloneMaskedCommand(from, to, destination, parseMode(inner.find("CLONE_MODE")));
                }
                case "REPLACE": {
                    return new CloneCommand(from, to, destination, parseMode(inner.find("CLONE_MODE")));
                }
                default: {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
                }
            }
        }
        return new CloneCommand(from, to, destination);
    }

    private static CloneCommand.SourceMode parseMode(TokenPattern<?> pattern) {
        if(pattern == null) return CloneCommand.SourceMode.NORMAL;
        return CloneCommand.SourceMode.valueOf(pattern.flatten(false).toUpperCase());
    }
}
