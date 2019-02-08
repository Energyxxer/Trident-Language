package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.advancement.AdvancementCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

import java.util.ArrayList;

@AnalyzerMember(key = "advancement")
public class AdvancementParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        AdvancementCommand.Action action = pattern.find("ACTION").flatten(false).equals("revoke") ? AdvancementCommand.Action.REVOKE : AdvancementCommand.Action.GRANT;
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
        TokenPattern<?> inner = ((TokenStructure) pattern.find("INNER")).getContents();
        switch(inner.getName()) {
            case "EVERYTHING":
                return new AdvancementCommand(action, entity, AdvancementCommand.Limit.EVERYTHING);
            case "FROM_THROUGH_UNTIL": {
                String rawLimit = inner.find("LIMIT").flatten(false);
                AdvancementCommand.Limit limit = rawLimit.equals("from") ? AdvancementCommand.Limit.FROM : rawLimit.equals("through") ? AdvancementCommand.Limit.THROUGH : AdvancementCommand.Limit.UNTIL;
                TridentUtil.ResourceLocation advancement = CommonParsers.parseResourceLocation(inner.find("RESOURCE_LOCATION"), ctx);
                advancement.assertStandalone(inner.find("RESOURCE_LOCATION"), ctx);
                return new AdvancementCommand(action, entity, limit, advancement.toString());
            }
            case "ONLY": {
                TridentUtil.ResourceLocation advancement = CommonParsers.parseResourceLocation(inner.find("RESOURCE_LOCATION"), ctx);
                advancement.assertStandalone(inner.find("RESOURCE_LOCATION"), ctx);
                TokenList criteriaList = (TokenList) (inner.find("CRITERIA.CRITERIA_LIST"));
                ArrayList<String> criteria = new ArrayList<>();
                if(criteriaList != null) {
                    for(TokenPattern<?> criterion : criteriaList.getContents()) {
                        if(!criterion.getName().equals("LINE_GLUE")) {
                            criteria.add(criterion.flatten(false));
                        }
                    }
                }
                try {
                    return new AdvancementCommand(action, entity, AdvancementCommand.Limit.ONLY, advancement.toString(), criteria);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx).map(CommodoreException.Source.ENTITY_ERROR, pattern.find("ENTITY")).invokeThrow();
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}
