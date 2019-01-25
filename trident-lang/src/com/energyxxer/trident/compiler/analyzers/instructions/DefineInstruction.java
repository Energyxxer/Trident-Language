package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;

@AnalyzerMember(key = "define")
public class DefineInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "DEFINE_OBJECTIVE":
                defineObjective(inner, file);
                break;
            case "DEFINE_ENTITY":
                CustomEntity.defineEntity(inner, file);
                break;
            case "DEFINE_ITEM":
                CustomItem.defineItem(inner, file);
                break;
            case "DEFINE_FUNCTION":
                TridentFile.createInnerFile(inner.find("INNER_FUNCTION"), file);
                break;
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
            }
        }
    }

    private void defineObjective(TokenPattern<?> pattern, TridentFile file) {
        String objectiveName = CommonParsers.parseIdentifierA(pattern.find("OBJECTIVE_NAME.IDENTIFIER_A"), file);
        String criteria = "dummy";
        TextComponent displayName = null;

        TokenPattern<?> sub = pattern.find("");
        if(sub != null) {
            criteria = CommonParsers.parseIdentifierB(sub.find("CRITERIA.IDENTIFIER_B"), file);
            TokenPattern<?> rawDisplayName = sub.find(".TEXT_COMPONENT");
            if(rawDisplayName != null) {
                displayName = TextParser.parseTextComponent(rawDisplayName, file);
            }
        }

        if(file.getCompiler().getModule().getObjectiveManager().contains(objectiveName)) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "An objective with the name '" + objectiveName + "' has already been defined", pattern));
        } else {
            file.getCompiler().getModule().getObjectiveManager().create(objectiveName, criteria, displayName, true);
        }
    }
}
