package com.energyxxer.enxlex.pattern_matching;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

public class ParsingSignature {
    private int hashCode;
    private TokenPattern<?> pattern;
    private SummaryModule summary;

    public ParsingSignature(int hashCode, TokenPattern<?> pattern, SummaryModule summary) {
        this.hashCode = hashCode;
        this.pattern = pattern;
        this.summary = summary;
    }

    public int getHashCode() {
        return hashCode;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public TokenPattern<?> getPattern() {
        return pattern;
    }

    public void setPattern(TokenPattern<?> pattern) {
        this.pattern = pattern;
    }

    public SummaryModule getSummary() {
        return summary;
    }

    public void setSummary(SummaryModule summary) {
        this.summary = summary;
    }
}
