package com.energyxxer.trident.global.temp;

import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.enxlex.lexical_analysis.EagerLexer;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.matching.GeneralTokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenPatternMatch;
import com.energyxxer.nbtmapper.parser.NBTTMLexerProfile;
import com.energyxxer.nbtmapper.parser.NBTTMProductions;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.global.temp.lang_defaults.parsing.MCFunctionProductions;
import com.energyxxer.trident.global.temp.lang_defaults.presets.JSONLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.MCFunctionLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.PropertiesLexerProfile;
import com.energyxxer.util.Factory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by User on 2/9/2017.
 */
public enum Lang {
    JSON(JSONLexerProfile::new, "json", "mcmeta", TridentCompiler.PROJECT_FILE_NAME.substring(1)),
    PROPERTIES(PropertiesLexerProfile::new, "properties", "lang"),
    MCFUNCTION(MCFunctionLexerProfile::new, MCFunctionProductions.FILE, "mcfunction"),
    TRIDENT(TridentLexerProfile::new, TridentProductions.FILE, "tdn"),
    NBTTM(() -> new NBTTMLexerProfile(StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_SNAPSHOT), NBTTMProductions.FILE, "nbttm");

    Factory<LexerProfile> factory;
    GeneralTokenPatternMatch parserProduction;
    List<String> extensions;

    Lang(Factory<LexerProfile> factory, String... extensions) {
        this(factory, null, extensions);
    }

    Lang(Factory<LexerProfile> factory, GeneralTokenPatternMatch parserProduction, String... extensions) {
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

    public GeneralTokenPatternMatch getParserProduction() {
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

    public boolean isLazy() {
        return parserProduction instanceof LazyTokenPatternMatch;
    }

    public Lexer createLexer(File file, String text) {
        if(isLazy()) {
            LazyLexer lexer = new LazyLexer(new TokenStream(true), (LazyTokenPatternMatch) parserProduction);
            lexer.tokenizeParse(file, text, createProfile());
            return lexer;
        } else {
            EagerLexer lexer = new EagerLexer(new TokenStream(true));
            lexer.tokenize(file, text, createProfile());
            return lexer;
        }
    }
}