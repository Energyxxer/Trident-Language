package com.energyxxer.trident.compiler.commands.parsers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "as")
public class AsParser implements ModifierParser {
    @Override
    public ExecuteModifier parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        return new ExecuteAsEntity(EntityParser.parseEntity(pattern.find("ENTITY"), compiler));
    }
}
