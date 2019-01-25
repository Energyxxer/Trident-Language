package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TeamArgument;
import com.energyxxer.commodore.types.defaults.TeamReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "team")
public class TeamArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> rawValue = pattern.find("IDENTIFIER_A");
        String str = "";
        if(rawValue != null) {
            str = CommonParsers.parseIdentifierA(rawValue, file);
        }
        return new TeamArgument(new TeamReference(str), pattern.find("NEGATED") != null);
    }
}
