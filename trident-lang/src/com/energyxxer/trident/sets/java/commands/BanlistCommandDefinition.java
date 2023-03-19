package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.ban.BanListCommand;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class BanlistCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"banlist"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("banlist"),
                choice(
                        literal("players").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> BanListCommand.QueryType.PLAYERS),
                        literal("ips").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> BanListCommand.QueryType.IPS)
                ).setOptional().setName("BANLIST_QUERY_TYPE")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        return new BanListCommand((BanListCommand.QueryType) pattern.findThenEvaluate("BANLIST_QUERY_TYPE", null, ctx, null));
    }
}
