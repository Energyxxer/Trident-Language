package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.inspection.CommandResolution;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "msg")
public class MsgParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file);
        String message = pattern.search(TridentTokens.TRAILING_STRING).get(0).value;

        return executionContext -> new CommandResolution(executionContext, "msg " + entity + " " + message);
    }

    @AnalyzerMember(key = "w")
    public static class MsgParserAlias0 extends MsgParser implements CommandParser {}

    @AnalyzerMember(key = "tell")
    public static class MsgParserAlias1 extends MsgParser implements CommandParser {}
}
