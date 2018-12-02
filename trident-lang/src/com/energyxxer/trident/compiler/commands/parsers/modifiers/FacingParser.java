package com.energyxxer.trident.compiler.commands.parsers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.EntityAnchor;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteFacingBlock;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteFacingEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;

import java.util.List;

@ParserMember(key = "facing")
public class FacingParser implements ModifierParser {
    @Override
    public ExecuteModifier parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> branch = ((TokenStructure) pattern.find("CHOICE")).getContents();
        switch(branch.getName()) {
            case "ENTITY_BRANCH": {
                List<Token> anchorToken = branch.search(TridentTokens.ANCHOR);
                return new ExecuteFacingEntity(EntityParser.parseEntity(branch.find("ENTITY"), compiler), (!anchorToken.isEmpty() && anchorToken.get(0).value.equals("eyes")) ? EntityAnchor.EYES : EntityAnchor.FEET);
            }
            case "BLOCK_BRANCH": {
                return new ExecuteFacingBlock(CoordinateParser.parse(branch.find("COORDINATE_SET")));
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + branch.getName() + "'", branch));
                return null;
            }
        }
    }
}
