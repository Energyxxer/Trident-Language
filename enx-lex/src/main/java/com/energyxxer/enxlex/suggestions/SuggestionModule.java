package com.energyxxer.enxlex.suggestions;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

public class SuggestionModule {

    public enum SuggestionStatus {
        ENABLED,
        DISABLED;
    }
    private LazyLexer lexer;

    private final int originalSuggestionIndex;
    private int suggestionIndex;
    private int caretIndex;
    private Stack<SuggestionStatus> statusStack = new Stack<>();

    private ArrayList<Suggestion> suggestions = new ArrayList<>();

    private String[] lookingAtMemberPath = null;

    public SuggestionModule(int suggestionIndex, int caretIndex) {
        originalSuggestionIndex = suggestionIndex;
        this.suggestionIndex = suggestionIndex;
        this.caretIndex = caretIndex;
    }

    public void addSuggestion(Suggestion prediction) {
        if(!suggestions.contains(prediction)) {
            if(prediction instanceof LiteralSuggestion && prediction.tags.contains(SuggestionTags.LITERAL_SORT)) {
                int insertionIndex = suggestions.size();
                Suggestion previousSuggestion;
                while(
                        insertionIndex-1 >= 0 &&
                        ((previousSuggestion = suggestions.get(insertionIndex-1)) instanceof LiteralSuggestion) &&
                                Collator.getInstance(Locale.ENGLISH).compare(((LiteralSuggestion) previousSuggestion).getLiteral(), ((LiteralSuggestion) prediction).getLiteral()) > 0
                ) {
                    insertionIndex--;
                }

                suggestions.add(insertionIndex, prediction);
            } else {
                suggestions.add(prediction);
            }
        }
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public int getOriginalSuggestionIndex() {
        return originalSuggestionIndex;
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
