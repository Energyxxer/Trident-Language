package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.selector.arguments.NameArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "name")
public class NameArgumentParser implements SelectorArgumentParser {
    @Override
    public SelectorArgument parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> rawValue = pattern.find("SELECTOR_ARGUMENT_VALUE");
        String str = "";
        if(rawValue != null) {
            str = rawValue.flatten(false);
            if(!rawValue.deepSearchByName("STRING_LITERAL").isEmpty()) {
                str = CommandUtils.parseQuotedString(str);
            }
        }
        return new NameArgument(str, pattern.find("NEGATED") != null);
    }
}
