package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.inspection.CommandResolution;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "msg")
public class MsgParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file);
        String message = pattern.search(TridentTokens.TRAILING_STRING).get(0).value;

        return executionContext -> new CommandResolution(executionContext, "msg \be0 " + message, entity);
    }

    @ParserMember(key = "w")
    public static class MsgParserAlias0 extends MsgParser implements CommandParser {}

    @ParserMember(key = "tell")
    public static class MsgParserAlias1 extends MsgParser implements CommandParser {}
}
