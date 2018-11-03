package com.energyxxer.trident.global.temp;

import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.parsing.MCFunctionProductions;
import com.energyxxer.trident.global.temp.lang_defaults.presets.JSONLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.MCFunctionLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.PropertiesLexerProfile;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.util.Factory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by User on 2/9/2017.
 */
public enum Lang {
    JSON(JSONLexerProfile::new, "json", "mcmeta"),
    PROPERTIES(PropertiesLexerProfile::new, "properties", "lang", "project"),
    MCFUNCTION(MCFunctionLexerProfile::new, MCFunctionProductions.FILE, "mcfunction"),
    TRIDENT(TridentLexerProfile::new, "tdn");

    Factory<LexerProfile> factory;
    TokenPatternMatch parserProduction;
    List<String> extensions;

    Lang(Factory<LexerProfile> factory, String... extensions) {
        this(factory, null, extensions);
    }

    Lang(Factory<LexerProfile> factory, TokenPatternMatch parserProduction, String... extensions) {
        this.factory = factory;
        this.parserProduction = parserProduction;
        this.extensions = Arrays.asList(extensions);
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public LexerProfile createProfile() {
        return factory.createInstance();
    }

    public TokenPatternMatch getParserProduction() {
        return parserProduction;
    }

    public static Lang getLangForFile(String path) {
        for(Lang lang : Lang.values()) {
            for(String extension : lang.extensions) {
                if(path.endsWith("." + extension)) {
                    return lang;
                }
            }
        }
        return null;
    }
}