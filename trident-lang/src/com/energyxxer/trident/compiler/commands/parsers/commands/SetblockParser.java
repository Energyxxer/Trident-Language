package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.setblock.SetblockCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "setblock")
public class SetblockParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        CoordinateSet pos = CoordinateParser.parse(pattern.find("COORDINATE_SET"), file);
        Block block = CommonParsers.parseBlock(pattern.find("BLOCK"), file);
        SetblockCommand.OldBlockHandlingMode mode = SetblockCommand.OldBlockHandlingMode.DEFAULT;

        if(!block.getBlockType().isStandalone()) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Block tags aren't allowed in this context", pattern.find("BLOCK")));
            throw new EntryParsingException();
        }

        TokenPattern<?> rawMode = pattern.find("OLD_BLOCK_HANDLING");
        if(rawMode != null) {
            mode = SetblockCommand.OldBlockHandlingMode.valueOf(rawMode.flatten(false).toUpperCase());
        }

        return new SetblockCommand(pos, block, mode);
    }
}
