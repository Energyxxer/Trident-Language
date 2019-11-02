package com.energyxxer.enxlex.suggestions;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class SuggestionModule {

    public enum SuggestionStatus {
        ENABLED,
        DISABLED;
    }
    private LazyLexer lexer;

    private int suggestionIndex;
    private int caretIndex;
    private Stack<SuggestionStatus> statusStack = new Stack<>();

    private ArrayList<Suggestion> suggestions = new ArrayList<>();

    private String[] lookingAtMemberPath = null;

    public SuggestionModule(int suggestionIndex, int caretIndex) {
        this.suggestionIndex = suggestionIndex;
        this.caretIndex = caretIndex;
    }

    public void addSuggestion(Suggestion prediction) {
        if(!suggestions.contains(prediction)) suggestions.add(prediction);
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public int getSuggestionIndex() {
        return suggestionIndex;
    }

    public void setSuggestionIndex(int index) {
        this.suggestionIndex = index;
    }

    public int getCaretIndex() {
        return caretIndex;
    }

    public void setCaretIndex(int caretIndex) {
        this.caretIndex = caretIndex;
    }

    public Lexer getLexer() {
        return lexer;
    }

    public void setLexer(LazyLexer lexer) {
        this.lexer = lexer;
    }

    public boolean isAtSuggestionIndex(int index) {
        return suggestionIndex >= index && suggestionIndex <= lexer.getLookingIndexTrimmed();
    }

    public boolean shouldSuggest() {
        return !statusStack.isEmpty() && statusStack.peek() == SuggestionStatus.ENABLED;
    }

    public void pushStatus(SuggestionStatus status) {
        statusStack.push(status);
    }

    public SuggestionStatus popStatus() {
        return statusStack.pop();
    }

    public void setLookingAtMemberPath(String[] lookingAtMemberPath) {
        this.lookingAtMemberPath = lookingAtMemberPath;
    }

    public String[] getLookingAtMemberPath() {
        return lookingAtMemberPath;
    }
}
