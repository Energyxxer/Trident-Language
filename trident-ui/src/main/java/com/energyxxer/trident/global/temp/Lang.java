package com.energyxxer.trident.global.temp;

import com.energyxxer.trident.global.temp.lang_defaults.parsing.MCFunctionProductions;
import com.energyxxer.trident.global.temp.lang_defaults.presets.JSONScannerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.MCFunctionScannerProfile;
import com.energyxxer.trident.global.temp.lang_defaults.presets.PropertiesScannerProfile;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerProfile;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.util.Factory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by User on 2/9/2017.
 */
public enum Lang {
    JSON(JSONScannerProfile::new, "json", "mcmeta"), PROPERTIES(PropertiesScannerProfile::new, "properties", "lang", "project"), MCFUNCTION(MCFunctionScannerProfile::new, MCFunctionProductions.FILE, "mcfunction");

    Factory<ScannerProfile> factory;
    TokenPatternMatch parserProduction;
    List<String> extensions;

    Lang(Factory<ScannerProfile> factory, String... extensions) {
        this(factory, null, extensions);
    }

    Lang(Factory<ScannerProfile> factory, TokenPatternMatch parserProduction, String... extensions) {
        this.factory = factory;
        this.parserProduction = parserProduction;
        this.extensions = Arrays.asList(extensions);
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public ScannerProfile createProfile() {
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