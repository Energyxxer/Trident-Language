package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.attribute.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import java.util.UUID;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class AttributeCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"attribute"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return TridentProductions.versionLimited(worker, "command.attribute", false, group(
                TridentProductions.commandHeader("attribute"),
                productions.getOrCreateStructure("ENTITY"),
                productions.getOrCreateStructure("ATTRIBUTE_ID"),
                choice(
                        group(literal("get"), TridentProductions.real(productions).setOptional().setName("SCALE").addTags("cspn:Scale")).setEvaluator(AttributeCommandDefinition::getBranch),
                        group(literal("base"),
                                choice(
                                        group(literal("get"), TridentProductions.real(productions).setOptional().setName("SCALE").addTags("cspn:Scale")).setEvaluator(AttributeCommandDefinition::baseGetBranch),
                                        group(literal("set"), TridentProductions.real(productions).setName("VALUE").addTags("cspn:Value")).setEvaluator(AttributeCommandDefinition::baseSetBranch)
                                )
                        ).setSimplificationFunctionContentIndex(1),
                        group(literal("modifier"),
                                choice(
                                        group(
                                                literal("add"),
                                                productions.getOrCreateStructure("UUID"),
                                                wrapper(
                                                        productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")
                                                ).setName("ATTRIBUTE_MODIFIER_NAME").addTags("cspn:Attribute Modifier Name"),
                                                TridentProductions.real(productions).setName("VALUE").addTags("cspn:Attribute Modifier Value"),
                                                enumChoice(AttributeModifierAddCommand.Operation.class).setName("ATTRIBUTE_MODIFIER_OPERATION")
                                        ).setEvaluator(AttributeCommandDefinition::baseModifierAddBranch),
                                        group(
                                                literal("value"),
                                                literal("get"),
                                                productions.getOrCreateStructure("UUID"),
                                                TridentProductions.real(productions).setOptional().setName("SCALE").addTags("cspn:Scale")
                                        ).setEvaluator(AttributeCommandDefinition::baseModifierGetBranch),
                                        group(literal("remove"), productions.getOrCreateStructure("UUID")).setEvaluator(AttributeCommandDefinition::baseModifierRemoveBranch)
                                )
                        ).setSimplificationFunctionContentIndex(1)
                ).setName("SUBCOMMAND")
            )
        );
    }

    private static Command getBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        Entity entity = (Entity) data[0];
        Type attributeType = (Type) data[1];
        double scale = (double) pattern.findThenEvaluate("SCALE", 1d, ctx, null);
        return new AttributeGetCommand(entity, attributeType, scale);
    }

    private static Command baseGetBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        Entity entity = (Entity) data[0];
        Type attributeType = (Type) data[1];
        double scale = (double) pattern.findThenEvaluate("SCALE", 1d, ctx, null);
        return new AttributeBaseGetCommand(entity, attributeType, scale);
    }

    private static Command baseSetBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        Entity entity = (Entity) data[0];
        Type attributeType = (Type) data[1];
        double value = (double) pattern.find("VALUE").evaluate(ctx, null);
        return new AttributeBaseSetCommand(entity, attributeType, value);
    }

    private static Command baseModifierAddBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        Entity entity = (Entity) data[0];
        Type attributeType = (Type) data[1];

        UUID uuid = (UUID) pattern.find("UUID").evaluate(ctx, null);
        String modifierName = (String) pattern.find("ATTRIBUTE_MODIFIER_NAME").evaluate(ctx, null);
        double value = (double) pattern.find("VALUE").evaluate(ctx, null);
        AttributeModifierAddCommand.Operation operation = (AttributeModifierAddCommand.Operation) pattern.find("ATTRIBUTE_MODIFIER_OPERATION").evaluate(ctx, null);
        return new AttributeModifierAddCommand(entity, attributeType, uuid, modifierName, value, operation);
    }

    private static Command baseModifierGetBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        Entity entity = (Entity) data[0];
        Type attributeType = (Type) data[1];

        UUID uuid = (UUID) pattern.find("UUID").evaluate(ctx, null);
        double scale = (double) pattern.findThenEvaluate("SCALE", 1d, ctx, null);
        return new AttributeModifierGetCommand(entity, attributeType, uuid, scale);
    }

    private static Command baseModifierRemoveBranch(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        Entity entity = (Entity) data[0];
        Type attributeType = (Type) data[1];

        UUID uuid = (UUID) pattern.find("UUID").evaluate(ctx, null);
        return new AttributeModifierRemoveCommand(entity, attributeType, uuid);
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx, null);
        Type attributeType = (Type) pattern.find("ATTRIBUTE_ID").evaluate(ctx, null);

        try {
            return (Command) pattern.find("SUBCOMMAND").evaluate(ctx, new Object[] {entity, attributeType});
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("ENTITY"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("ATTRIBUTE_ID"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
