package com.energyxxer.enxlex.lexical_analysis;

import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.suggestions.SuggestionModule;
import com.energyxxer.enxlex.report.Notice;

import java.util.ArrayList;

public abstract class Lexer {

    protected TokenStream stream;

    protected ArrayList<Notice> notices = new ArrayList<>();

    protected SuggestionModule suggestionModule = null;

    public TokenStream getStream() {
        return stream;
    }

    public ArrayList<Notice> getNotices() {
        return notices;
    }

    public SuggestionModule getSuggestionModule() {
        return suggestionModule;
    }

    public void setSuggestionModule(SuggestionModule suggestionModule) {
        this.suggestionModule = suggestionModule;
    }
}
