package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.teammsg.TeamMessageCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "teammsg")
public class TeamMsgParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = pattern.search(TridentTokens.TRAILING_STRING).get(0).value;

        try {
            return new TeamMessageCommand(message);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    @AnalyzerMember(key = "tm")
    public static class TeamMsgParserAlias0 extends TeamMsgParser implements SimpleCommandParser {}
}
