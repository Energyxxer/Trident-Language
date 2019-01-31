package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAlignment;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "align")
public class AlignParser implements SimpleModifierParser {
    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, TridentFile file) {
        String swizzle = pattern.search(TridentTokens.SWIZZLE).get(0).value;
        return new ExecuteAlignment(swizzle.contains("x"), swizzle.contains("y"), swizzle.contains("z"));
    }
}
