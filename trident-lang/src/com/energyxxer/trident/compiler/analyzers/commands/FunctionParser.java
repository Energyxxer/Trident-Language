package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "function")
public class FunctionParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> choice = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(choice.getName()) {
            case "RESOURCE_LOCATION_TAGGED": {
                return new FunctionCommand(CommonParsers.parseFunctionTag((TokenStructure) choice, ctx));
            }
            case "ANONYMOUS_INNER_FUNCTION": {
                TridentFile inner = TridentFile.createInnerFile(choice, ctx);
                return new FunctionCommand(inner.getFunction());
            }
            default: {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Unknown grammar branch name '" + choice.getName() + "'", choice, ctx);
            }
        }
    }
}
