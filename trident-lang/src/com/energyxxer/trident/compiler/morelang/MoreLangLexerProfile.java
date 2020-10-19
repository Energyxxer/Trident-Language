package com.energyxxer.trident.compiler.morelang;

import com.energyxxer.enxlex.lexical_analysis.profiles.*;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.Lazy;

import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.molang.MoLangTokens.*;

public class MoreLangLexerProfile extends LexerProfile {

    public static final Lazy<MoreLangLexerProfile> INSTANCE = new Lazy<>(MoreLangLexerProfile::new);

    private static final Pattern numberRegex = Pattern.compile("(-?\\d+(\\.\\d+)?)");

    public MoreLangLexerProfile() {
        this.initialize();
    }

    private void initialize() {

        //Symbols
        contexts.add(new StringTypeMatchLexerContext(new String[] { ".", ",", ";", "(", ")", "[", "]", "{", "}", "->" },
                new TokenType[] { DOT, COMMA, SEMICOLON, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, ARROW }
        ));

        //Operators
        contexts.add(new StringMatchLexerContext(OPERATOR, "+", "-", "*", "/", "??", "!", "?", ":", "==", "!=", "<=", ">=", "<", ">", "="));


        //String literals
        contexts.add(new StringLiteralLexerContext("'", STRING_LITERAL));

        //Comments
        contexts.add(new CommentLexerContext("//", COMMENT));

        contexts.add(new IdentifierLexerContext(IDENTIFIER, "[a-zA-Z0-9_]", "[a-zA-Z_]").setOnlyWhenExpected(false));

        contexts.add(new RegexLexerContext(numberRegex, NUMBER, true));
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","morelang");
        header.attributes.put("DESC","Mo(re)Lang Script File");
    }
}
