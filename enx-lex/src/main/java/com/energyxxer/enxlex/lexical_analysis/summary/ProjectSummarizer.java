package com.energyxxer.enxlex.lexical_analysis.summary;

import com.energyxxer.enxlex.pattern_matching.ParsingSignature;

import java.util.HashMap;

public interface ProjectSummarizer {
    void setSourceCache(HashMap<String, ParsingSignature> cache);
    HashMap<String, ParsingSignature> getSourceCache();
    void addCompletionListener(java.lang.Runnable r);
    ProjectSummary getSummary();
    void start();
}
