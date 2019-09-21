package com.energyxxer.enxlex.lexical_analysis.summary;

import com.energyxxer.enxlex.lexical_analysis.token.Token;

public class Todo {
    private Token token;
    private String text;

    public Todo(Token token, String text) {
        this.token = token;
        this.text = text;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
