package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.whitelist.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "whitelist")
public class WhitelistParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "WHITELIST_ON": {
                return new SetWhitelistEnabledCommand(true);
            }
            case "WHITELIST_OFF": {
                return new SetWhitelistEnabledCommand(false);
            }
            case "WHITELIST_RELOAD": {
                return new WhitelistReloadCommand();
            }
            case "WHITELIST_LIST": {
                return new WhitelistListCommand();
            }
            case "WHITELIST_ADD":
            case "WHITELIST_REMOVE": {

                Entity profile = EntityParser.parseEntity(inner.find("ENTITY"), ctx);

                try {
                    if (inner.getName().equals("WHITELIST_REMOVE")) {
                        return new WhitelistRemoveCommand(profile);
                    } else {
                        return new WhitelistAddCommand(profile);
                    }
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.find("ENTITY"))
                            .map(CommodoreException.Source.FORMAT_ERROR, inner.find("ENTITY"))
                            .invokeThrow();
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}
