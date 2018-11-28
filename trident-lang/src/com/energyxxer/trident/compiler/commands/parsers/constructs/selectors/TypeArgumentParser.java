package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "type")
public class TypeArgumentParser implements SelectorArgumentParser {
    @Override
    public SelectorArgument parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> rawValue = pattern.find("SELECTOR_ARGUMENT_VALUE");
        TridentUtil.ResourceLocation typeLoc = new TridentUtil.ResourceLocation(rawValue.find("ENTITY_ID"));
        Type type = compiler.getModule().getNamespace(typeLoc.namespace).types.entity.get(typeLoc.body);
        return new TypeArgument(type, rawValue.find("NEGATED") != null);
    }
}