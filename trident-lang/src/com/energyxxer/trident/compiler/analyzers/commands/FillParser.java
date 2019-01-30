package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.fill.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "fill")
public class FillParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        CoordinateSet from = CoordinateParser.parse(pattern.find("FROM.COORDINATE_SET"), file);
        CoordinateSet to = CoordinateParser.parse(pattern.find("TO.COORDINATE_SET"), file);
        Block block = CommonParsers.parseBlock(pattern.find("BLOCK"), file);
        FillCommand.FillMode mode = new FillReplaceMode();

        TokenPattern<?> inner = pattern.find("CHOICE");
        if(inner != null) {
            inner = ((TokenStructure) inner).getContents();
            switch(inner.getName()) {
                case "LITERAL_DESTROY": {
                    mode = new FillDestroyMode();
                    break;
                }
                case "LITERAL_HOLLOW": {
                    mode = new FillHollowMode();
                    break;
                }
                case "LITERAL_KEEP": {
                    mode = new FillKeepMode();
                    break;
                }
                case "LITERAL_OUTLINE": {
                    mode = new FillOutlineMode();
                    break;
                }
                case "REPLACE": {
                    Block replaceBlock = CommonParsers.parseBlock(inner.find(".BLOCK_TAGGED"), file);
                    if(replaceBlock != null) mode = new FillReplaceMode(replaceBlock);
                    break;
                }
                default: {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
                }
            }
        }

        try {
            return new FillCommand(from, to, block, mode);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, file)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("BLOCK"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, file);
        }
    }
}
