package com.energyxxer.enxlex.lexical_analysis;

import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.report.Notice;

import java.util.ArrayList;

public abstract class Lexer {

    protected TokenStream stream;

    protected ArrayList<Notice> notices = new ArrayList<>();

    public TokenStream getStream() {
        return stream;
    }

    public ArrayList<Notice> getNotices() {
        return notices;
    }
}
