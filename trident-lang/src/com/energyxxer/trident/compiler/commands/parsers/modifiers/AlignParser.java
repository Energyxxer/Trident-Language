package com.energyxxer.trident.compiler.commands.parsers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAlignment;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;

@ParserMember(key = "align")
public class AlignParser implements ModifierParser {
    @Override
    public ExecuteModifier parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        String swizzle = pattern.search(TridentTokens.SWIZZLE).get(0).value;
        return new ExecuteAlignment(swizzle.contains('x'), swizzle.contains('y'), swizzle.contains('z'));
    }
}
