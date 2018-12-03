package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.DZArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "dz")
public class DZArgumentParser implements SelectorArgumentParser {
    @Override
    public SelectorArgument parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        return new DZArgument(Double.parseDouble(pattern.flatten(false)));
    }
}
