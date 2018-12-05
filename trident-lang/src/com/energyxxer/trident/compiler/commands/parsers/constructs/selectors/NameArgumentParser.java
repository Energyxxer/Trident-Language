package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.NameArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "name")
public class NameArgumentParser implements SelectorArgumentParser {
    @Override
    public SelectorArgument parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        return new NameArgument(CommonParsers.parseStringLiteralOrIdentifierA(pattern.find(".STRING_LITERAL_OR_IDENTIFIER_A")), pattern.find("NEGATED") != null);
    }
}
