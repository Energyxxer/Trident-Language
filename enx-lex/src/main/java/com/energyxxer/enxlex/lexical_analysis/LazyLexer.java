package com.energyxxer.enxlex.lexical_analysis;

import com.energyxxer.enxlex.lexical_analysis.profiles.LexerContext;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenPatternMatch;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.util.StringLocation;
import com.energyxxer.util.StringLocationCache;

import java.io.File;

public class LazyLexer extends Lexer {

    private LazyTokenPatternMatch pattern;

    public LazyLexer(TokenStream stream, LazyTokenPatternMatch pattern) {
        this.stream = stream;
        this.pattern = pattern;
    }

    private int currentIndex = 0;
    private String fileContents = null;
    private StringLocationCache lineCache = new StringLocationCache();
    private LexerProfile profile = null;

    private File file;

    private TokenMatchResponse matchResponse = null;

    public void tokenizeParse(File file, String str, LexerProfile profile) {
        this.file = file;
        this.fileContents = str;
        this.profile = profile;

        lineCache.setText(fileContents);
        lineCache.prepopulate();

        {
            Token header = new Token("", TokenType.FILE_HEADER, file, new StringLocation(0, 0, 0));
            profile.putHeaderInfo(header);
            stream.write(header);
        }

        matchResponse = pattern.match(0, this);

        if(matchResponse.matched) {
            //Debug.log("Successfully matched: " + matchResponse.pattern);
            matchResponse.pattern.validate();
            for(Token token : matchResponse.pattern.flattenTokens()) {
                stream.write(token);
            }
        } else {
            this.notices.add(new Notice(NoticeType.ERROR, matchResponse.getErrorMessage(), matchResponse.faultyToken));
            //Debug.log("Did not match:" + matchResponse.faultyToken + " | " + matchResponse.faultyToken.loc + " | expected " + matchResponse.expected);
        }


        {
            Token eof = new Token("", TokenType.END_OF_FILE, file, lineCache.getLocationForOffset(fileContents.length()));
            stream.write(eof);
        }
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public String getLookingAt() {
        return fileContents.substring(currentIndex);
    }

    public String getLookingAtTrimmed() {
        return fileContents.substring(getLookingIndexTrimmed());
    }

    public int getLookingIndexTrimmed() {
        int index = currentIndex;
        while(index < fileContents.length() && Character.isWhitespace(fileContents.charAt(index))) index++;
        return index;
    }

    public Token retrieveTokenOfType(TokenType type) {
        for (LexerContext context : profile.contexts) {
            if (context.getHandledTypes().contains(type)) {
                ScannerContextResponse response = context.analyzeExpectingType(context.ignoreLeadingWhitespace() ?
                        getLookingAtTrimmed() :
                        getLookingAt(), type, profile);
                /*if (response.errorMessage != null) {
                    notices.add(new Notice(NoticeType.ERROR, response.errorMessage, "\b" + file.getAbsolutePath() + "\b" + (getLookingIndexTrimmed() + response.errorIndex) + "\b" + response.errorLength));
                }*/
                if (response.success && response.tokenType == type) {
                    return new Token(response.value, response.tokenType, file, lineCache.getLocationForOffset(context.ignoreLeadingWhitespace() ?
                            getLookingIndexTrimmed() :
                            getCurrentIndex()), response.subSections);
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
        if (type == TokenType.UNKNOWN) {
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

    public Token retrieveAnyToken() {
        for (LexerContext context : profile.contexts) {
            ScannerContextResponse response = context.analyze(
                    context.ignoreLeadingWhitespace() ?
                            getLookingAtTrimmed() :
                            getLookingAt(),
                    profile);
            /*if (response.errorMessage != null) {
                notices.add(new Notice(NoticeType.ERROR, response.errorMessage, "\b" + file.getAbsolutePath() + "\b" + (getLookingIndexTrimmed() + response.errorIndex) + "\b" + response.errorLength));
            }*/
            if (response.success) {
                return new Token(response.value, response.tokenType, file, lineCache.getLocationForOffset(
                        context.ignoreLeadingWhitespace() ?
                                getLookingIndexTrimmed() :
                                getCurrentIndex()
                ), response.subSections);
            }
        }
        if(getLookingIndexTrimmed() == fileContents.length()) {
            return new Token("", TokenType.END_OF_FILE, file, lineCache.getLocationForOffset(fileContents.length()));
        }
        {
            int index = currentIndex;
            while(index < fileContents.length() && fileContents.charAt(index) != '\n' && Character.isWhitespace(fileContents.charAt(index))) index++;
            if(fileContents.charAt(index) == '\n') return new Token("\n", TokenType.NEWLINE, file, lineCache.getLocationForOffset(index));
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
            if(sb.length() > 0) return new Token(sb.toString(), TokenType.UNKNOWN, file, lineCache.getLocationForOffset(getLookingIndexTrimmed()));
        }
        return null;
    }

    public int getFileLength() {
        return fileContents.length();
    }

    public TokenMatchResponse getMatchResponse() {
        return matchResponse;
    }
}
