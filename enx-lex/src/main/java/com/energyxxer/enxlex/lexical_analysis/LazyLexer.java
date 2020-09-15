package com.energyxxer.enxlex.lexical_analysis;

import com.energyxxer.enxlex.lexical_analysis.profiles.LexerContext;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.suggestions.SuggestionModule;
import com.energyxxer.util.StringLocation;
import com.energyxxer.util.StringLocationCache;

import java.io.File;

public class LazyLexer extends Lexer {

    private TokenPatternMatch pattern;

    public LazyLexer(TokenStream stream, TokenPatternMatch pattern) {
        this.stream = stream;
        this.pattern = pattern;
    }

    private String fileContents = null;
    private StringLocationCache lineCache = new StringLocationCache();
    private LexerProfile profile = null;

    private File file;

    private TokenMatchResponse matchResponse = null;

    @Override
    public void start(File file, String str, LexerProfile profile) {
        this.file = file;
        this.fileContents = str;
        this.profile = profile;

        if(getSummaryModule() != null) getSummaryModule().onStart();

        lineCache.setText(fileContents);
        lineCache.prepopulate();

        {
            Token header = new Token("", TokenType.FILE_HEADER, file, new StringLocation(0, 0, 0));
            profile.putHeaderInfo(header);
            stream.write(header);
        }

        matchResponse = pattern.match(0, this);

        if(matchResponse.matched) {
            matchResponse.pattern.validate();
            for(Token token : matchResponse.pattern.flattenTokens()) {
                notices.addAll(token.attachedNotices);
                stream.write(token);
            }
        } else {
            this.notices.add(new Notice(NoticeType.ERROR, matchResponse.getErrorMessage(), matchResponse.faultyToken));
        }

        {
            Token eof = new Token("", TokenType.END_OF_FILE, file, lineCache.getLocationForOffset(fileContents.length()));
            stream.write(eof);
        }

        if(getSummaryModule() != null) getSummaryModule().onEnd();
    }

    public String getCurrentReadingString() {
        return fileContents;
    }

    public String getLookingAt() {
        return fileContents.substring(currentIndex);
    }

    public String getLookingAtTrimmed() {
        return fileContents.substring(getLookingIndexTrimmed());
    }

    @Override
    public int getLookingIndexTrimmed() {
        int index = currentIndex;
        while(index < fileContents.length() && Character.isWhitespace(fileContents.charAt(index))) index++;
        return index;
    }

    @Override
    public Token retrieveTokenOfType(TokenType type) {
        int lookingIndexTrimmed = getLookingIndexTrimmed();
        for (LexerContext context : profile.contexts) {
            if (context.getHandledTypes().contains(type)) {
                ScannerContextResponse response = context.analyzeExpectingType(fileContents, context.ignoreLeadingWhitespace() ?
                        lookingIndexTrimmed :
                        currentIndex, type, profile);
                if (response.success && response.tokenType == type) {
                    Token token = new Token(response.value, response.tokenType, file, lineCache.getLocationForOffset(context.ignoreLeadingWhitespace() ?
                            getLookingIndexTrimmed() :
                            getCurrentIndex()), response.subSections);
                    if (response.errorMessage != null) {
                        token.attachedNotices.add(new Notice(NoticeType.ERROR, response.errorMessage, "\b" + file.getAbsolutePath() + "\b" + (getLookingIndexTrimmed() + response.errorIndex) + "\b" + response.errorLength));
                    }
                    return token;
                }
            }
        }
        if (type == TokenType.END_OF_FILE) {
            if(getLookingIndexTrimmed() == fileContents.length()) {
                return new Token("", TokenType.END_OF_FILE, file, lineCache.getLocationForOffset(fileContents.length()));
            }
        }
        if(type == TokenType.NEWLINE) {
            int index = currentIndex;
            while(index < fileContents.length() && fileContents.charAt(index) != '\n' && Character.isWhitespace(fileContents.charAt(index))) index++;
            if(index < fileContents.length() && fileContents.charAt(index) == '\n') return new Token("\n", TokenType.NEWLINE, file, lineCache.getLocationForOffset(index));
        }
        if (type == TokenType.UNKNOWN || type == null) {
            StringBuilder sb = new StringBuilder();
            for (int i = getLookingIndexTrimmed(); i < fileContents.length(); i++) {

                char lastChar = '\u0000';

                if (i > 0) lastChar = fileContents.charAt(i - 1);

                if (sb.length() > 0 && lastChar != '\u0000' && !profile.canMerge(lastChar, fileContents.charAt(i))) {
                    return new Token(sb.toString(), TokenType.UNKNOWN, file, lineCache.getLocationForOffset(getLookingIndexTrimmed()));
                }
                sb.append(fileContents.charAt(i));
            }
            if(sb.length() > 0) return new Token(sb.toString(), TokenType.UNKNOWN, file, lineCache.getLocationForOffset(getLookingIndexTrimmed()));
        }
        return null;
    }

    @Override
    public Token retrieveAnyToken() {
        int lookingIndexTrimmed = getLookingIndexTrimmed();
        for (LexerContext context : profile.contexts) {
            ScannerContextResponse response = context.analyze(
                    fileContents,
                    context.ignoreLeadingWhitespace() ?
                            lookingIndexTrimmed :
                            currentIndex,
                    profile);
            if (response.success && !response.value.isEmpty()) {
                Token token = new Token(response.value, response.tokenType, file, lineCache.getLocationForOffset(
                        context.ignoreLeadingWhitespace() ?
                                getLookingIndexTrimmed() :
                                getCurrentIndex()
                ), response.subSections);
                if (response.errorMessage != null) {
                    token.attachedNotices.add(new Notice(NoticeType.ERROR, response.errorMessage, "\b" + file.getAbsolutePath() + "\b" + (getLookingIndexTrimmed() + response.errorIndex) + "\b" + response.errorLength));
                }
                return token;
            }
        }
        if(getLookingIndexTrimmed() == fileContents.length()) {
            return new Token("", TokenType.END_OF_FILE, file, lineCache.getLocationForOffset(fileContents.length()));
        }
        {
            StringBuilder sb = new StringBuilder();
            for (int i = getLookingIndexTrimmed(); i < fileContents.length(); i++) {

                char lastChar = '\u0000';

                if (i > 0) lastChar = fileContents.charAt(i - 1);

                if (sb.length() > 0 && lastChar != '\u0000' && !profile.canMerge(lastChar, fileContents.charAt(i))) {
                    return new Token(sb.toString(), TokenType.UNKNOWN, file, lineCache.getLocationForOffset(getLookingIndexTrimmed()));
                }
                sb.append(fileContents.charAt(i));
            }
            if(sb.length() > 0) {
                return new Token(sb.toString(), TokenType.UNKNOWN, file, lineCache.getLocationForOffset(getLookingIndexTrimmed()));
            }
        }
        return null;
    }

    @Override
    public int getFileLength() {
        return fileContents.length();
    }

    public TokenMatchResponse getMatchResponse() {
        return matchResponse;
    }

    @Override
    public void setSuggestionModule(SuggestionModule suggestionModule) {
        super.setSuggestionModule(suggestionModule);
        suggestionModule.setLexer(this);
    }
}
