package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.whitelist.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class WhitelistCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"whitelist"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("whitelist"),
                choice(
                        literal("on").setEvaluator((p, d) -> new SetWhitelistEnabledCommand(true)),
                        literal("off").setEvaluator((p, d) -> new SetWhitelistEnabledCommand(false)),
                        literal("list").setEvaluator((p, d) -> new WhitelistListCommand()),
                        literal("reload").setEvaluator((p, d) -> new WhitelistReloadCommand()),
                        group(literal("add"), wrapper(productions.getOrCreateStructure("ENTITY"), (v, p, d) -> {
                            try {
                                return new WhitelistAddCommand((Entity) v);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0]).invokeThrow();
                                return null;
                            }
                        })).setSimplificationFunctionContentIndex(1),
                        group(literal("remove"), wrapper(productions.getOrCreateStructure("ENTITY"), (v, p, d) -> {
                            try {
                                return new WhitelistRemoveCommand((Entity) v);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0]).invokeThrow();
                                return null;
                            }
                        })).setSimplificationFunctionContentIndex(1)
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
