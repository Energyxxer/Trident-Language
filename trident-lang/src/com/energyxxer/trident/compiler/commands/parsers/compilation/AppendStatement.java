package com.energyxxer.trident.compiler.commands.parsers.compilation;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.commands.parsers.commands.CommandParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "command_append")
public class AppendStatement implements CompileStatement {
    @Override
    public void execute(TokenPattern<?> pattern, CompilationBlock context) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("COMMAND")).getContents();
        CommandParser commandParser = ParserManager.getParser(CommandParser.class, inner.flattenTokens().get(0).value);
        if(commandParser != null) {
            Command command = commandParser.parse(inner, context.file);
            if(command != null) {
                context.function.append(command);
            }
        }
    }
}
