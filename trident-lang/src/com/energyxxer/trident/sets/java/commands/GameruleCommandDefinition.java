package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.gamerule.GameruleQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.gamerule.GameruleSetCommand;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.choice;
import static com.energyxxer.prismarine.PrismarineProductions.group;

public class GameruleCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"gamerule"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("gamerule"),
                choice(
                        productions.getOrCreateStructure("GAMERULE_ID"),
                        productions.getOrCreateStructure("GAMERULE_SETTER")
                ).setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Object inner = pattern.find("INNER").evaluate(ctx);
        if (inner instanceof Type) {
            return new GameruleQueryCommand((Type) inner);
        } else {
            return (GameruleSetCommand) inner;
        }
    }
}
