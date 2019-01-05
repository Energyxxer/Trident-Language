package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.advancement.AdvancementCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.ArrayList;

@ParserMember(key = "advancement")
public class AdvancementParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        AdvancementCommand.Action action = pattern.find("ACTION").flatten(false).equals("revoke") ? AdvancementCommand.Action.REVOKE : AdvancementCommand.Action.GRANT;
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file);
        TokenPattern<?> inner = ((TokenStructure) pattern.find("INNER")).getContents();
        switch(inner.getName()) {
            case "EVERYTHING":
                return new AdvancementCommand(action, entity, AdvancementCommand.Limit.EVERYTHING);
            case "FROM_THROUGH_UNTIL": {
                String rawLimit = inner.find("LIMIT").flatten(false);
                AdvancementCommand.Limit limit = rawLimit.equals("from") ? AdvancementCommand.Limit.FROM : rawLimit.equals("through") ? AdvancementCommand.Limit.THROUGH : AdvancementCommand.Limit.UNTIL;
                TridentUtil.ResourceLocation advancement = new TridentUtil.ResourceLocation(inner.search(TridentTokens.RESOURCE_LOCATION).get(0).value);
                return new AdvancementCommand(action, entity, limit, advancement.toString());
            }
            case "ONLY": {
                TridentUtil.ResourceLocation advancement = new TridentUtil.ResourceLocation(inner.search(TridentTokens.RESOURCE_LOCATION).get(0).value);
                TokenList criteriaList = (TokenList) (inner.find("CRITERIA.CRITERIA_LIST"));
                ArrayList<String> criteria = new ArrayList<>();
                if(criteriaList != null) {
                    for(TokenPattern<?> criterion : criteriaList.getContents()) {
                        if(!criterion.getName().equals("LINE_GLUE")) {
                            criteria.add(criterion.flatten(false));
                        }
                    }
                }
                return new AdvancementCommand(action, entity, AdvancementCommand.Limit.ONLY, advancement.toString(), criteria);
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }
}
