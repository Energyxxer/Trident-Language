package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.AdvancementArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCompletionEntry;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCriterionEntry;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCriterionGroupEntry;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "advancements")
public class AdvancementArgumentParser implements SimpleSelectorArgumentParser, SelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, TridentFile file) {
        TokenList advancementList = (TokenList) pattern.find("ADVANCEMENT_LIST");

        AdvancementArgument advancements = new AdvancementArgument();

        if(advancementList != null) {
            for(TokenPattern<?> rawArg : advancementList.getContents()) {
                if(rawArg.getName().equals("ADVANCEMENT_ENTRY")) {
                    TridentUtil.ResourceLocation advancementLoc = new TridentUtil.ResourceLocation(rawArg.find("ADVANCEMENT_ENTRY_KEY").flatten(false));
                    TokenPattern<?> rawValue = ((TokenStructure)rawArg.find("ADVANCEMENT_ENTRY_VALUE")).getContents();
                    switch(rawValue.getName()) {
                        case "BOOLEAN": {
                            advancements.addEntry(new AdvancementCompletionEntry(advancementLoc.toString(), rawValue.flatten(false).equals("true")));
                            break;
                        }
                        case "CRITERION_GROUP": {
                            AdvancementCriterionGroupEntry criteria = parseCriterionGroup(rawValue, file, advancementLoc);
                            advancements.addEntry(criteria);
                            break;
                        }
                        default: {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + rawValue.getName() + "'", rawValue));
                            return null;
                        }
                    }
                }
            }
        }

        return advancements;
    }

    private AdvancementCriterionGroupEntry parseCriterionGroup(TokenPattern<?> pattern, TridentFile file, TridentUtil.ResourceLocation advancementLoc) {
        AdvancementCriterionGroupEntry criteria = new AdvancementCriterionGroupEntry(advancementLoc.toString());
        TokenList criterionList = (TokenList) pattern.find("CRITERION_LIST");
        if(criterionList != null) {
            for(TokenPattern<?> rawArg : criterionList.getContents()) {
                if(rawArg.getName().equals("CRITERION_ENTRY")) {
                    String criterionName = rawArg.find("CRITERION_NAME").flatten(false);
                    boolean value = rawArg.find("BOOLEAN").flatten(false).equals("true");
                    criteria.addCriteria(new AdvancementCriterionEntry(criterionName, value));
                }
            }
        }
        return criteria;
    }
}
