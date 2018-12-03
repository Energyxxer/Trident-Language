package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.DXArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "dx")
public class DXArgumentParser implements SelectorArgumentParser {
    @Override
    public SelectorArgument parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        return new DXArgument(Double.parseDouble(pattern.flatten(false)));
    }
}
