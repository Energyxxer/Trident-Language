package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.locatebiome.LocateBiomeCommand;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.trident.compiler.TridentProductions.commandHeader;

public class LocateBiomeCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"locatebiome"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                commandHeader("locatebiome"),
                productions.getOrCreateStructure("BIOME_ID")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        return new LocateBiomeCommand((Type) pattern.find("BIOME_ID").evaluate(ctx));
    }
}
