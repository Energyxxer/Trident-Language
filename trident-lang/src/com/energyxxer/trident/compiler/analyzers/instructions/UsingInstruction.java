package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionVariable;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionVariableMap;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.*;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.commands.SummonParser;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;

@AnalyzerMember(key = "using")
public class UsingInstruction implements Instruction {
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
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + usingCase.getName() + "'", usingCase, ctx);
            }
        }
    }

    private void usingTag(TokenPattern<?> pattern, ISymbolContext ctx, TokenPattern<?> executionBlock) {
        ctx.assertLanguageLevel(2, "The using-tag instruction is", pattern);
        FunctionSection function = ctx.getWritingFile().getFunction();

        String tag = CommonParsers.parseIdentifierA(pattern.find("USING_TAG_NAME.IDENTIFIER_A"), ctx);

        Entity startEntity = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
        ArrayList<ExecuteModifier> modifiers = CommonParsers.parseModifierList(((TokenList) pattern.find("MODIFIER_LIST")), ctx);

        if(modifiers.isEmpty()) {
            function.append(new TagCommand(TagCommand.Action.ADD, startEntity, tag));
        } else {
            modifiers.add(0, new ExecuteAsEntity(startEntity));
            function.append(new ExecuteCommand(new TagCommand(TagCommand.Action.ADD, new Selector(Selector.BaseSelector.SENDER), tag), modifiers));
        }

        IfInstruction.resolveBlock(executionBlock, ctx);

        function.append(new TagCommand(TagCommand.Action.REMOVE, TridentUtil.getTopLevelEntity(startEntity), tag));
    }

    private void usingSummon(TokenPattern<?> pattern, ISymbolContext ctx, TokenPattern<?> executionBlock) {
        ctx.assertLanguageLevel(2, "The using-summon instruction is", pattern);
        FunctionSection function = ctx.getWritingFile().getFunction();

        String tag = CommonParsers.parseIdentifierA(pattern.find("USING_SUMMON_TAG_NAME.IDENTIFIER_A"), ctx);

        SummonParser.SummonData data = new SummonParser.SummonData(pattern, ctx,
                pattern.find("ENTITY_ID"),
                pattern.find(".COORDINATE_SET"),
                pattern.find("..NBT_COMPOUND"),
                ((TokenList) pattern.find("IMPLEMENTED_FEATURES.FEATURE_LIST")));

        ArrayList<ExecuteModifier> modifiers = CommonParsers.parseModifierList(((TokenList) pattern.find("MODIFIER_LIST")), ctx);

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
        for(CustomEntity feature : data.features) {
            summoned.addArguments(new TagArgument(feature.getIdTag()));
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
            double epsilon = 0.01;
            summoned.addArguments(new DistanceArgument(new NumberRange<>(Math.max(distance-epsilon, 0), distance+epsilon)));
        }

        Command tagRemoveCommand = new TagCommand(TagCommand.Action.REMOVE, new Selector(Selector.BaseSelector.SENDER), tag);
        Command innerCallCommand = new FunctionCommand(innerFile.getFunction());

        if(collapse) {
            innerFile.getFunction().prepend(tagRemoveCommand);
            modifiers.add(0, new ExecuteAsEntity(summoned));
            function.append(new ExecuteCommand(innerCallCommand, modifiers));
        } else {
            Function middleFunction = TridentFile.createAnonymousSubFunction(ctx);
            middleFunction.append(tagRemoveCommand);
            middleFunction.append(new ExecuteCommand(innerCallCommand, modifiers));
            function.append(new ExecuteCommand(new FunctionCommand(middleFunction), new ExecuteAsEntity(summoned)));
        }

        //ctx.getStaticParentFile().
    }
}
