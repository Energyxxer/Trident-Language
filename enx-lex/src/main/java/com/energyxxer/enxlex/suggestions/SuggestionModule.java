package com.energyxxer.enxlex.suggestions;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class SuggestionModule {

    public enum SuggestionStatus {
        ENABLED,
        DISABLED
    }

    private LazyLexer lexer;
    private int suggestionIndex;
    private int caretIndex;

    private Stack<SuggestionStatus> statusStack = new Stack<>();

    private Set<Suggestion> suggestions = new HashSet<>();

    public SuggestionModule(int suggestionIndex, int caretIndex) {
        this.suggestionIndex = suggestionIndex;
        this.caretIndex = caretIndex;
    }

    public void addSuggestion(Suggestion prediction) {
        suggestions.add(prediction);
    }

    public Collection<Suggestion> getSuggestions() {
        return suggestions;
    }

    public int getSuggestionIndex() {
        return suggestionIndex;
    }

    public int getCaretIndex() {
        return caretIndex;
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
}
