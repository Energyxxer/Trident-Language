package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TeamArgument;
import com.energyxxer.commodore.types.defaults.TeamReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "team")
public class TeamArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> rawValue = pattern.find("IDENTIFIER_A");
        TeamReference team = null;
        if(rawValue != null) {
            team = new TeamReference(CommonParsers.parseIdentifierA(rawValue, ctx));
        }
        return new TeamArgument(team, pattern.find("NEGATED") != null);
    }
}
