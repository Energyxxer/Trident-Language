package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.ban.BanCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "ban")
public class BanParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity player = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
        String reason = null;
        if(pattern.find("REASON") != null) {
            reason = pattern.find("REASON").flatten(false);
        }

        try {
            return new BanCommand(player, reason);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("ENTITY"))
                    .map(CommodoreException.Source.FORMAT_ERROR, pattern.find("ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
