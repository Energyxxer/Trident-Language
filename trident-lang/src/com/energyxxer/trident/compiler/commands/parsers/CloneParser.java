package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneCommand;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneFilteredCommand;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneMaskedCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "clone")
public class CloneParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        CoordinateSet from = CoordinateParser.parse(pattern.find("FROM.COORDINATE_SET"));
        CoordinateSet to = CoordinateParser.parse(pattern.find("TO.COORDINATE_SET"));
        CoordinateSet destination = CoordinateParser.parse(pattern.find("DESTINATION.COORDINATE_SET"));

        TokenPattern<?> inner = pattern.find("CHOICE");
        if(inner != null) {
            inner = ((TokenStructure)inner).getContents();
            switch(inner.getName()) {
                case "FILTERED": {
                    Block block = CommonParsers.parseBlock(inner.find("BLOCK_TAGGED"), file.getCompiler());
                    return new CloneFilteredCommand(from, to, destination, block, parseMode(inner.find("CLONE_MODE")));
                }
                case "MASKED": {
                    return new CloneMaskedCommand(from, to, destination, parseMode(inner.find("CLONE_MODE")));
                }
                case "REPLACE": {
                    return new CloneCommand(from, to, destination, parseMode(inner.find("CLONE_MODE")));
                }
                default: {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'"));
                    return null;
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
