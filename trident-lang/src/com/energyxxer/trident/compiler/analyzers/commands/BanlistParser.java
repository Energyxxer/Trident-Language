package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.ban.BanListCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "banlist")
public class BanlistParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern.find("BANLIST_QUERY_TYPE") == null) return new BanListCommand();
        switch(pattern.find("BANLIST_QUERY_TYPE").flatten(false)) {
            case "ips": {
                return new BanListCommand(BanListCommand.QueryType.IPS);
            }
            case "players": {
                return new BanListCommand(BanListCommand.QueryType.PLAYERS);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
            }
        }
    }
}
