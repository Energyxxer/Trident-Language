package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.CommandDefinition;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

import java.util.Collection;
import java.util.Collections;

import static com.energyxxer.prismarine.PrismarineProductions.enumChoice;
import static com.energyxxer.prismarine.PrismarineProductions.group;

public class ComponentCommandDefinition implements CommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"component"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("component"),
                productions.getOrCreateStructure("ENTITY"),
                enumChoice(TagCommand.Action.class).setName("COMPONENT_ACTION"),
                TridentProductions.noToken().addTags("cspn:Component"),
                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"), false, CustomEntity.class).setName("COMPONENT")
        );
    }

    @Override
    public Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx, Collection<ExecuteModifier> modifiers) {
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx, null);
        TagCommand.Action action = (TagCommand.Action) pattern.find("COMPONENT_ACTION").evaluate(ctx, null);

        CustomEntity component = (CustomEntity) pattern.find("COMPONENT").evaluate(ctx, null);
        if (!component.isComponent()) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Expected entity component, instead got custom entity", pattern, ctx);
        }

        return Collections.singletonList(new TagCommand(action, entity, component.getIdTag()));
    }
}
