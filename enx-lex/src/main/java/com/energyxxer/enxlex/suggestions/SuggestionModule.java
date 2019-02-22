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
    private int focusedIndex;

    private Stack<SuggestionStatus> statusStack = new Stack<>();

    private Set<Suggestion> suggestions = new HashSet<>();

    public SuggestionModule(int focusedIndex) {
        this.focusedIndex = focusedIndex;
    }

    public void addSuggestion(Suggestion prediction) {
        suggestions.add(prediction);
    }

    public Collection<Suggestion> getSuggestions() {
        return suggestions;
    }

    public int getFocusedIndex() {
        return focusedIndex;
    }

    public Lexer getLexer() {
        return lexer;
    }

    public void setLexer(LazyLexer lexer) {
        this.lexer = lexer;
    }

    public boolean isAtFocusedIndex(int index) {
        return focusedIndex >= index && focusedIndex <= lexer.getLookingIndexTrimmed();
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
