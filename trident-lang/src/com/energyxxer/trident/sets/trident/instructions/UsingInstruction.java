package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionVariable;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionVariableMap;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.*;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.sets.trident.TridentLiteralSet;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import java.util.ArrayList;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class UsingInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(TridentProductions.instructionKeyword("using"),
                choice(
                        group(literal("tag"), wrapper(TridentProductions.identifierA(productions)).setName("USING_TAG_NAME").addTags("cspn:Tag"), productions.getOrCreateStructure("ENTITY"), productions.getOrCreateStructure("MODIFIER_LIST")).setName("USING_TAG"),
                        group(literal("summon"), productions.getOrCreateStructure("NEW_ENTITY_LITERAL"), optional(productions.getOrCreateStructure("COORDINATE_SET"), optional(productions.getOrCreateStructure("NBT_COMPOUND"))), literal("with"), wrapper(TridentProductions.identifierA(productions)).setName("USING_SUMMON_TAG_NAME").addTags("cspn:Summoning Tag"), productions.getOrCreateStructure("MODIFIER_LIST")).setName("USING_SUMMON")
                ).setName("USING_CASE"),
                productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")
        );
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> executionBlock = pattern.find("ANONYMOUS_INNER_FUNCTION");

        TokenPattern<?> usingCase = ((TokenStructure) pattern.find("USING_CASE")).getContents();
        switch(usingCase.getName()) {
            case "USING_TAG": {
                usingTag(usingCase, ctx, executionBlock);
                break;
            }
            case "USING_SUMMON": {
                usingSummon(usingCase, ctx, executionBlock);
                break;
            }
            default: {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + usingCase.getName() + "'", usingCase, ctx);
            }
        }
    }

    private void usingTag(TokenPattern<?> pattern, ISymbolContext ctx, TokenPattern<?> executionBlock) {
        TridentUtil.assertLanguageLevel(ctx, 2, "The using-tag instruction is", pattern);
        FunctionSection function = ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction();

        String tag = (String) pattern.find("USING_TAG_NAME").evaluate(ctx);

        Entity startEntity = (Entity) pattern.find("ENTITY").evaluate(ctx);

        ArrayList<ExecuteModifier> modifiers = (ArrayList<ExecuteModifier>) pattern.findThenEvaluateLazyDefault("MODIFIER_LIST", ArrayList::new, ctx);
        modifiers.addAll(0, ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getWritingModifiers());

        if(modifiers.isEmpty()) {
            function.append(new TagCommand(TagCommand.Action.ADD, startEntity, tag));
        } else {
            modifiers.add(ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getWritingModifiers().size(), new ExecuteAsEntity(startEntity));
            function.append(new ExecuteCommand(new TagCommand(TagCommand.Action.ADD, new Selector(Selector.BaseSelector.SENDER), tag), modifiers));
        }

        IfInstruction.resolveBlock(executionBlock, ctx);

        function.append(new ExecuteCommand(new TagCommand(TagCommand.Action.REMOVE, TridentUtil.getTopLevelEntity(startEntity), tag), ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getWritingModifiers()));
    }

    private void usingSummon(TokenPattern<?> pattern, ISymbolContext ctx, TokenPattern<?> executionBlock) {
        TridentUtil.assertLanguageLevel(ctx, 2, "The using-summon instruction is", pattern);
        FunctionSection function = ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction();

        String tag = (String) pattern.find("USING_SUMMON_TAG_NAME").evaluate(ctx);

        TridentLiteralSet.SummonData data = (TridentLiteralSet.SummonData) pattern.find("NEW_ENTITY_LITERAL").evaluate(ctx);
        data.pos = (CoordinateSet) pattern.findThenEvaluate(".COORDINATE_SET", null, ctx);
        data.mergeNBT((TagCompound) pattern.findThenEvaluate("..NBT_COMPOUND", null, ctx));
        data.analyzeNBT(pattern, ctx);
        data.mergeNBT(new TagCompound(new TagList("Tags", new TagString(tag))));
        function.append(new ExecuteCommand(data.constructSummon(), ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getWritingModifiers()));

        data.fillDefaults();


        ArrayList<ExecuteModifier> modifiers = (ArrayList<ExecuteModifier>) pattern.findThenEvaluateLazyDefault("MODIFIER_LIST", ArrayList::new, ctx);

        TridentFile innerFile = TridentFile.createInnerFile(executionBlock, ctx);

        //collapse: whether to add the tag @s remove into the inner file or not
        boolean collapse = true;
        for(ExecuteModifier modifier : modifiers) {
            ExecutionVariableMap map = modifier.getModifiedExecutionVariables();
            if(map.isUsed(ExecutionVariable.SENDER) || map.isUsed(ExecutionVariable.CONDITION)) {
                collapse = false;
            }
            if(!collapse && modifier instanceof ExecuteAtEntity) {
                Entity atEntity = ((ExecuteAtEntity) modifier).getEntity();
                if(atEntity instanceof Selector) {
                    Selector atSelector = ((Selector) atEntity);
                    if(atSelector.getBase() == Selector.BaseSelector.SENDER && atSelector.getAllArguments().isEmpty()) {
                        collapse = true;
                    }
                }
            }
            if(!collapse) break;
        }

        Selector summoned = new Selector(Selector.BaseSelector.ALL_ENTITIES);
        summoned.addArgument(new TypeArgument(data.type));
        if(data.reference instanceof CustomEntity) {
            CustomEntity ce = ((CustomEntity) data.reference);
            summoned.addArgument(new TagArgument(ce.getIdTag()));
        }
        for(CustomEntity component : data.components) {
            summoned.addArguments(new TagArgument(component.getIdTag()));
        }
        summoned.addArguments(new TagArgument(tag));
        summoned.addArguments(new LimitArgument(1));

        if (data.pos.getX().getType() != Coordinate.Type.LOCAL && data.pos.getY().getType() != Coordinate.Type.LOCAL && data.pos.getZ().getType() != Coordinate.Type.LOCAL) {
            double xDist = data.pos.getX().getCoord(); //relative by default
            if(data.pos.getX().getType() == Coordinate.Type.ABSOLUTE) {
                summoned.addArguments(new XArgument(data.pos.getX().getCoord()));
                xDist = 0;
            }
            double yDist = data.pos.getY().getCoord(); //relative by default
            if(data.pos.getY().getType() == Coordinate.Type.ABSOLUTE) {
                summoned.addArguments(new YArgument(data.pos.getY().getCoord()));
                yDist = 0;
            }
            double zDist = data.pos.getZ().getCoord(); //relative by default
            if(data.pos.getZ().getType() == Coordinate.Type.ABSOLUTE) {
                summoned.addArguments(new ZArgument(data.pos.getZ().getCoord()));
                zDist = 0;
            }
            double distance = Math.sqrt(xDist*xDist + yDist*yDist + zDist*zDist);
            double margin = 0.01;
            if("true".equals(data.type.getProperty("snaps_to_grid"))) {
                margin += Math.sqrt(3);
            }
            summoned.addArguments(new DistanceArgument(new DoubleRange(Math.max(distance-margin, 0), distance+margin)));
        }

        Command tagRemoveCommand = new TagCommand(TagCommand.Action.REMOVE, new Selector(Selector.BaseSelector.SENDER), tag);
        Command innerCallCommand = new FunctionCommand(innerFile.getFunction());

        if(collapse) {
            innerFile.getFunction().prepend(tagRemoveCommand);
            modifiers.add(0, new ExecuteAsEntity(summoned));
            modifiers.addAll(0, ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getWritingModifiers());
            function.append(new ExecuteCommand(innerCallCommand, modifiers));
        } else {
            Function middleFunction = TridentFile.createAnonymousSubFunction(ctx);
            middleFunction.append(tagRemoveCommand);
            middleFunction.append(new ExecuteCommand(innerCallCommand, modifiers));

            ArrayList<ExecuteModifier> outerModifiers = new ArrayList<>(ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getWritingModifiers());
            outerModifiers.add(new ExecuteAsEntity(summoned));

            function.append(new ExecuteCommand(new FunctionCommand(middleFunction), outerModifiers));
        }
    }
}
