package com.energyxxer.trident.compiler.commands.parsers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.EntityAnchor;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAnchor;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;

@ParserMember(key = "anchored")
public class AnchoredParser implements ModifierParser {
    @Override
    public ExecuteModifier parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        return new ExecuteAnchor(pattern.search(TridentTokens.ANCHOR).get(0).value.equals("eyes") ? EntityAnchor.EYES : EntityAnchor.FEET);
    }
}
