package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.NameArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "name")
public class NameArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, TridentFile file) {
        return new NameArgument(CommonParsers.parseStringLiteralOrIdentifierA(pattern.find(".STRING_LITERAL_OR_IDENTIFIER_A"), file), pattern.find("NEGATED") != null);
    }
}
