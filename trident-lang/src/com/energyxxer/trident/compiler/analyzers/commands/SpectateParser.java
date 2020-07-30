package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.spectate.SpectateStartCommand;
import com.energyxxer.commodore.functionlogic.commands.spectate.SpectateStopCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "spectate")
public class SpectateParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            if (pattern.find("INNER") == null) return new SpectateStopCommand();
            if (pattern.find("INNER.INNER") == null) return new SpectateStartCommand(EntityParser.parseEntity(pattern.find("INNER.ENTITY"), ctx));
            return new SpectateStartCommand(EntityParser.parseEntity(pattern.find("INNER.ENTITY"), ctx), EntityParser.parseEntity(pattern.find("INNER.INNER.ENTITY"), ctx));
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map("TARGET", pattern.tryFind("INNER.ENTITY"))
                    .map("SPECTATOR", pattern.tryFind("INNER.INNER.ENTITY"))
                    .invokeThrow();
        }
        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
    }
}
