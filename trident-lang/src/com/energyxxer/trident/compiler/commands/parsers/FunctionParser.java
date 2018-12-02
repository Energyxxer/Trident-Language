package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "function")
public class FunctionParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        return new FunctionCommand(CommonParsers.parseFunctionTag(pattern.find("RESOURCE_LOCATION_TAGGED"), file.getCompiler()));
    }
}
