package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.kill.KillCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;
import static com.energyxxer.prismarine.PrismarineProductions.*;

public class KillCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"kill"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("kill"),
                optional(TridentProductions.sameLine(), productions.getOrCreateStructure("ENTITY")).setSimplificationFunctionContentIndex(1).setName("ENTITY")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity entity = (Entity) pattern.findThenEvaluateLazyDefault("ENTITY", () -> new Selector(SENDER), ctx);
        return new KillCommand(entity);
    }
}
