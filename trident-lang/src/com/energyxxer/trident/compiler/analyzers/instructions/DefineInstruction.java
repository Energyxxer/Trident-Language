package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;

@AnalyzerMember(key = "define")
public class DefineInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "DEFINE_OBJECTIVE":
                defineObjective(inner, ctx);
                break;
            case "DEFINE_ENTITY":
                CustomEntity.defineEntity(inner, ctx);
                break;
            case "DEFINE_ITEM":
                CustomItem.defineItem(inner, ctx);
                break;
            case "DEFINE_FUNCTION":
                TridentFile.createInnerFile(inner.find("INNER_FUNCTION"), ctx);
                break;
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    private void defineObjective(TokenPattern<?> pattern, ISymbolContext ctx) {
        String objectiveName = CommonParsers.parseIdentifierA(pattern.find("OBJECTIVE_NAME.IDENTIFIER_A"), ctx);
        String criteria = "dummy";
        TextComponent displayName = null;

        TokenPattern<?> sub = pattern.find("");
        if(sub != null) {
            criteria = CommonParsers.parseIdentifierB(sub.find("CRITERIA.IDENTIFIER_B"), ctx);
            TokenPattern<?> rawDisplayName = sub.find(".TEXT_COMPONENT");
            if(rawDisplayName != null) {
                displayName = TextParser.parseTextComponent(rawDisplayName, ctx);
            }
        }

        if(ctx.getCompiler().getModule().getObjectiveManager().contains(objectiveName)) {
            if(!ctx.getCompiler().getModule().getObjectiveManager().get(objectiveName).getType().equals(criteria)) {
                throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "An objective with the name '" + objectiveName + "' of a different type has already been defined", pattern, ctx);
            } else {
                ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "An objective with the name '" + objectiveName + "' has already been defined", pattern));
            }
        } else {
            ctx.getCompiler().getModule().getObjectiveManager().create(objectiveName, criteria, displayName, true);
        }
    }
}
