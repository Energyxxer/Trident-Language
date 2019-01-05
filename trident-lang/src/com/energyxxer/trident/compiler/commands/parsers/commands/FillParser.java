package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.fill.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "fill")
public class FillParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        CoordinateSet from = CoordinateParser.parse(pattern.find("FROM.COORDINATE_SET"), file);
        CoordinateSet to = CoordinateParser.parse(pattern.find("TO.COORDINATE_SET"), file);
        Block block = CommonParsers.parseBlock(pattern.find("BLOCK"), file);
        if(!block.getBlockType().isStandalone()) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Block tags aren't allowed in this context", pattern.find("BLOCK")));
            throw new EntryParsingException();
        }
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
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                    return null;
                }
            }
        }

        return new FillCommand(from, to, block, mode);
    }
}
