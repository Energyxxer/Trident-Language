package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "function")
public class FunctionParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> choice = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(choice.getName()) {
            case "RESOURCE_LOCATION_TAGGED": {
                return new FunctionCommand(CommonParsers.parseFunctionTag((TokenStructure) choice, file));
            }
            case "ANONYMOUS_INNER_FUNCTION": {
                TridentFile inner = TridentFile.createInnerFile(choice, file);
                return new FunctionCommand(inner.getFunction());
            }
            default: {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Unknown grammar branch name '" + choice.getName() + "'", choice, file);
            }
        }
    }
}
