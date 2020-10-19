package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.trident.compiler.semantics.custom.entities.EntityEvent;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;

import static com.energyxxer.trident.compiler.TridentProductions.commandHeader;
import static com.energyxxer.prismarine.PrismarineProductions.group;

public class EventCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"event"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                commandHeader("event"),
                productions.getOrCreateStructure("ENTITY"),
                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"), false, EntityEvent.class).setName("EVENT")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx);
        EntityEvent event = (EntityEvent) pattern.find("EVENT").evaluate(ctx);

        return new ExecuteCommand(new FunctionCommand(event.getFunction()), new ExecuteAsEntity(entity));
    }
}
