package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

public class CommentLexerContext implements LexerContext {
    private final String commentStart;
    private final TokenType handledType;

    public CommentLexerContext(String commentStart, TokenType handledType) {
        this.commentStart = commentStart;
        this.handledType = handledType;

    }

    @Override
    public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
        if(!str.startsWith(commentStart, startIndex)) return ScannerContextResponse.FAILED;
        if(str.indexOf("\n", startIndex) != -1) {
            return handleComment(str.substring(startIndex, str.indexOf("\n", startIndex)));
        } else return handleComment(str.substring(startIndex));
    }

    private ScannerContextResponse handleComment(String str) {
        HashMap<TokenSection, String> sections = new HashMap<>();
        int todoIndex = str.toUpperCase(Locale.ENGLISH).indexOf("TODO");
        if(todoIndex >= 0) {
            int todoEnd = str.indexOf("\n");
            if(todoEnd < 0) todoEnd = str.length();
            sections.put(new TokenSection(todoIndex, todoEnd-todoIndex), "comment.todo");
        }
        return new ScannerContextResponse(true, str, handledType, sections);
    }

    @Override
    public ContextCondition getCondition() {
        return ContextCondition.LINE_START;
    }

    @Override
    public Collection<TokenType> getHandledTypes() {
        return Collections.singletonList(handledType);
    }
}
