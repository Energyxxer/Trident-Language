package com.energyxxer.enxlex.lexical_analysis;

import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.suggestions.SuggestionModule;

import java.io.File;
import java.util.ArrayList;

public abstract class Lexer {

    protected TokenStream stream;

    protected ArrayList<Notice> notices = new ArrayList<>();

    protected SuggestionModule suggestionModule = null;
    protected SummaryModule summaryModule = null;

    protected int currentIndex = 0;

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }


    public TokenStream getStream() {
        return stream;
    }

    public ArrayList<Notice> getNotices() {
        return notices;
    }

    public SuggestionModule getSuggestionModule() {
        return suggestionModule;
    }

    public abstract void start(File file, String str, LexerProfile profile);

    public void setSuggestionModule(SuggestionModule suggestionModule) {
        this.suggestionModule = suggestionModule;
    }

    public SummaryModule getSummaryModule() {
        return summaryModule;
    }

    public void setSummaryModule(SummaryModule summaryModule) {
        this.summaryModule = summaryModule;
    }




    public abstract int getLookingIndexTrimmed();

    public abstract Token retrieveTokenOfType(TokenType type);
    public abstract Token retrieveAnyToken();

    public abstract int getFileLength();
}
