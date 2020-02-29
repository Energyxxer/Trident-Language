package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;
import java.util.Collections;

@AnalyzerMember(key = "component")
public class ComponentParser implements CommandParser {
    @Override
    public Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx, Collection<ExecuteModifier> modifiers) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);

        TagCommand.Action action = pattern.find("COMPONENT_ACTION.LITERAL_REMOVE") != null ? TagCommand.Action.REMOVE : TagCommand.Action.ADD;

        CustomEntity component = InterpolationManager.parse(pattern.find("LINE_SAFE_INTERPOLATION_VALUE"), ctx, CustomEntity.class);
        if(!component.isComponent()) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected entity component, instead got custom entity", pattern, ctx);
        }

        return Collections.singletonList(new TagCommand(action, entity, component.getIdTag()));
    }
}
