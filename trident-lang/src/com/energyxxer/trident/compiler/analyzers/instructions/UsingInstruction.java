package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.modifiers.ModifierParser;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.Collection;

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
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + usingCase.getName() + "'", usingCase, ctx);
            }
        }
    }

    private void usingTag(TokenPattern<?> pattern, ISymbolContext ctx, TokenPattern<?> executionBlock) {
        String tag = CommonParsers.parseIdentifierA(pattern.find("USING_TAG_NAME.IDENTIFIER_A"), ctx);

        Entity startEntity = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();

        TokenList rawModifierList = ((TokenList) pattern.find("MODIFIER_LIST"));
        if(rawModifierList != null) {
            for(TokenPattern<?> rawModifier : rawModifierList.getContents()) {
                ModifierParser parser = AnalyzerManager.getAnalyzer(ModifierParser.class, rawModifier.flattenTokens().get(0).value);
                if(parser != null) {
                    Collection<ExecuteModifier> modifier = parser.parse(rawModifier, ctx);
                    modifiers.addAll(modifier);
                } else {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown modifier analyzer for '" + rawModifier.flattenTokens().get(0).value + "'", rawModifier, ctx);
                }
            }
        }

        FunctionSection function = ctx.getWritingFile().getFunction();
        if(modifiers.isEmpty()) {
            function.append(new TagCommand(TagCommand.Action.ADD, startEntity, tag));
        } else {
            modifiers.add(0, new ExecuteAsEntity(startEntity));
            function.append(new ExecuteCommand(new TagCommand(TagCommand.Action.ADD, new Selector(Selector.BaseSelector.SENDER), tag), modifiers));
        }

        IfInstruction.resolveBlock(executionBlock, ctx);

        function.append(new TagCommand(TagCommand.Action.REMOVE, TridentUtil.getTopLevelEntity(startEntity), tag));
    }
}
