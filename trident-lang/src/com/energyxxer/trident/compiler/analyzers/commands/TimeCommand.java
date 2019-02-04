package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.time.TimeAddCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeSetCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "time")
public class TimeCommand implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "QUERY": {
                return new TimeQueryCommand(TimeQueryCommand.TimeCounter.valueOf(inner.find("CHOICE").flatten(false).toUpperCase()));
            }
            case "ADD": {
                try {
                    return new TimeAddCommand(CommonParsers.parseTime(inner.find("TIME"), ctx));
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, inner.find("TIME"))
                            .invokeThrow();
                }
            }
            case "SET": {
                TokenPattern<?> sub = ((TokenStructure) inner.find("CHOICE")).getContents();
                switch(sub.getName()) {
                    case "CHOICE": {
                        return new TimeSetCommand(TimeSetCommand.TimeOfDay.valueOf(sub.flatten(false).toUpperCase()));
                    }
                    case "TIME": {
                        try {
                            return new TimeSetCommand(CommonParsers.parseTime(sub, ctx));
                        } catch(CommodoreException x) {
                            TridentException.handleCommodoreException(x, pattern, ctx)
                                    .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, sub)
                                    .invokeThrow();
                        }
                    }
                    default: {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + sub.getName() + "'", sub, ctx);
                    }
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}
