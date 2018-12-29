package com.energyxxer.trident.global.temp;

import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.enxlex.lexical_analysis.EagerLexer;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.GeneralTokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenPatternMatch;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.parser.NBTTMLexerProfile;
import com.energyxxer.nbtmapper.parser.NBTTMProductions;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.global.temp.lang_defaults.parsing.MCFunctionProductions;
import com.energyxxer.trident.global.temp.lang_defaults.presets.JSONLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.MCFunctionLexerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.PropertiesLexerProfile;
import com.energyxxer.util.Factory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by User on 2/9/2017.
 */
public enum Lang {
    JSON(JSONLexerProfile::new, "json", "mcmeta", TridentCompiler.PROJECT_FILE_NAME.substring(1)),
    PROPERTIES(PropertiesLexerProfile::new, "properties", "lang"),
    MCFUNCTION(MCFunctionLexerProfile::new, () -> MCFunctionProductions.FILE, "mcfunction"),
    TRIDENT(() -> TridentLexerProfile.INSTANCE.getValue(), () -> Commons.getActiveTridentProductions(), "tdn"),
    NBTTM(() -> new NBTTMLexerProfile(StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_SNAPSHOT), () -> NBTTMProductions.FILE, "nbttm");

    Factory<LexerProfile> factory;
    Factory<GeneralTokenPatternMatch> parserProduction;
    List<String> extensions;

    Lang(Factory<LexerProfile> factory, String... extensions) {
        this(factory, null, extensions);
    }

    Lang(Factory<LexerProfile> factory, Factory<GeneralTokenPatternMatch> parserProduction, String... extensions) {
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

    public Factory<GeneralTokenPatternMatch> getParserProduction() {
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

    public LangAnalysisResponse analyze(File file, String text) {
        GeneralTokenPatternMatch patternMatch = (parserProduction != null) ? parserProduction.createInstance() : null;

        Lexer lexer;
        TokenMatchResponse response = null;
        ArrayList<Notice> notices = new ArrayList<>();
        ArrayList<Token> tokens;

        if(patternMatch instanceof LazyTokenPatternMatch) {
            lexer = new LazyLexer(new TokenStream(true), (LazyTokenPatternMatch) patternMatch);
            ((LazyLexer)lexer).tokenizeParse(file, text, createProfile());
            notices.addAll(lexer.getNotices());

            tokens = new ArrayList<>(lexer.getStream().tokens);
            tokens.remove(0);

            response = ((LazyLexer) lexer).getMatchResponse();
        } else {
            lexer = new EagerLexer(new TokenStream(true));
            ((EagerLexer)lexer).tokenize(file, text, createProfile());
            notices.addAll(lexer.getNotices());

            tokens = new ArrayList<>(lexer.getStream().tokens);
            tokens.remove(0);

            if(patternMatch != null) {

                response = ((TokenPatternMatch) patternMatch).match(tokens);

                if(response != null && !response.matched) {
                    notices.add(new Notice(NoticeType.ERROR, response.getErrorMessage(), response.faultyToken));
                }
            }
        }

        return new LangAnalysisResponse(lexer, response, tokens, notices);
    }

    public class LangAnalysisResponse {
        public Lexer lexer;
        public TokenMatchResponse response;
        public ArrayList<Token> tokens;
        public ArrayList<Notice> notices;

        public LangAnalysisResponse(Lexer lexer, TokenMatchResponse response, ArrayList<Token> tokens, ArrayList<Notice> notices) {
            this.lexer = lexer;
            this.response = response;
            this.tokens = tokens;
            this.notices = notices;
        }
    }
}