package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "tag")
public class TagArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> rawValue = pattern.find("IDENTIFIER_A");
        String str = "";
        if(rawValue != null) {
            str = rawValue.flatten(false);
            if(!rawValue.deepSearchByName("STRING_LITERAL").isEmpty()) {
                str = CommandUtils.parseQuotedString(str);
            }
        }
        return new TagArgument(str, pattern.find("NEGATED") != null);
    }
}
