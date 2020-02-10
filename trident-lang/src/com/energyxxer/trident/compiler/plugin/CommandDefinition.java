package com.energyxxer.trident.compiler.plugin;

import com.energyxxer.enxlex.lexical_analysis.EagerLexer;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.lexer.syntaxlang.TDNMetaLexerProfile;
import com.energyxxer.trident.compiler.lexer.syntaxlang.TDNMetaProductions;

import java.io.File;
import java.io.IOException;

public class CommandDefinition {
    private final String commandName;
    private final String rawSyntaxFile;
    private final String rawHandlerFile;
    private TokenPattern<?> rawSyntaxPattern;
    private TokenPattern<?> rawHandlerPattern;

    public CommandDefinition(String commandName, String rawSyntaxFile, String rawHandlerFile) {
        this.commandName = commandName;
        this.rawSyntaxFile = rawSyntaxFile;
        this.rawHandlerFile = rawHandlerFile;
    }

    void parseSyntaxFile(EagerLexer lexer, File file) throws IOException {
        lexer.tokenize(file, rawSyntaxFile, new TDNMetaLexerProfile());

        lexer.getStream().tokens.remove(0);

        TokenMatchResponse response = TDNMetaProductions.FILE.match(lexer.getStream().tokens);

        if(!response.matched) {
            throw new IOException("Syntax error in TDNMeta file '" + file + "': " + response.getErrorMessage());
        }

        rawSyntaxPattern = response.pattern;
    }

    void parseHandlerFile(LazyLexer lexer, File file) throws IOException {
        lexer.tokenizeParse(file, rawHandlerFile, new TridentLexerProfile());

        lexer.getStream().tokens.remove(0);

        TokenMatchResponse response = lexer.getMatchResponse();

        if(!response.matched) {
            throw new IOException("Syntax error in command handler file '" + file + "': " + response.getErrorMessage());
        }

        rawHandlerPattern = response.pattern;
    }

    public void createSyntax(TridentProductions productions) throws IOException {
        if(rawSyntaxPattern == null) return;
        TDNMetaBuilder builder = new TDNMetaBuilder(rawSyntaxPattern, productions);
        try {
            builder.build();
        } catch(TDNMetaBuilder.TDNMetaException x) {
            throw new IOException("Parsing error in TDNMeta file: " + x.getErrorMessage() + "; Caused by: " + x.getCausedBy().getLocation());
        }
        productions.registerCustomCommand(commandName, builder.getReturnValue());
    }

    public String getCommandName() {
        return commandName;
    }

    public TokenPattern<?> getRawSyntaxPattern() {
        return rawSyntaxPattern;
    }

    public TokenPattern<?> getRawHandlerPattern() {
        return rawHandlerPattern;
    }
}
