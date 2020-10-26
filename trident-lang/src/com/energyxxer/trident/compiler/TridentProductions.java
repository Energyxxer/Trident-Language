package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatures;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.plugin.TridentPluginUnitConfiguration;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.sets.BasicLiteralSet;
import com.energyxxer.trident.sets.MinecraftLiteralSet;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.plugins.PrismarinePlugin;
import com.energyxxer.prismarine.plugins.PrismarinePluginUnit;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;
import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TridentProductions {

    public static final TokenPatternMatch resourceLocationFixer = ofType(NO_TOKEN).setName("_RLCF").setOptional().addFailProcessor((p, l) -> {
        if(l.getSuggestionModule() != null) {
            if(l.getCurrentIndex() <= l.getSuggestionModule().getSuggestionIndex()+1) {
                int targetIndex = l.getLookingIndexTrimmed();
                String str = ((LazyLexer) l).getCurrentReadingString();
                int index = l.getSuggestionModule().getSuggestionIndex();

                if(index > 0) {
                    while (true) {
                        char c = str.charAt(index-1);
                        if (!(Character.isJavaIdentifierPart(c) || "#:/.-".contains(c+"")) || --index <= 1)
                            break;
                    }
                }

                if(index == targetIndex) {
                    l.getSuggestionModule().setSuggestionIndex(index);
                }
            }
        }
    });

    public static TokenPatternMatch semicolon() {
        return symbol(";").addTags(SuggestionTags.DISABLED);
    }

    public static TokenItemMatch symbol(String text) {
        return new TokenItemMatch(SYMBOL, text).setName("SYMBOL");
    }

    public static TokenItemMatch keyword(String text) {
        return matchItem(KEYWORD, text).setName("KEYWORD_" + text.toUpperCase());
    }

    public static TokenItemMatch instructionKeyword(String text) {
        return instructionKeyword(text, true);
    }

    public static TokenItemMatch instructionKeyword(String text, boolean updateContext) {
        TokenItemMatch item = keyword(text).setName("INSTRUCTION_KEYWORD");
        if(updateContext) item.addTags(TridentSuggestionTags.TAG_INSTRUCTION);
        return item;
    }

    public static TokenItemMatch brace(String brace) {
        TokenItemMatch item = matchItem(BRACE, brace);
        item.addTags(SuggestionTags.DISABLED);
        return item;
    }

    public static TokenItemMatch colon() {
        return ofType(COLON);
    }

    public static TokenItemMatch comma() {
        TokenItemMatch item = ofType(COMMA).setName("COMMA");
        item.addTags(SuggestionTags.DISABLED);
        return item;
    }

    public static TokenItemMatch dot() {
        TokenItemMatch item = ofType(DOT);
        item.addTags(SuggestionTags.DISABLED);
        return item;
    }

    public static TokenItemMatch equals() {
        return ofType(EQUALS);
    }

    public static TokenItemMatch caret() {
        return ofType(CARET);
    }

    public static TokenItemMatch tilde() {
        return ofType(TILDE);
    }

    public static TokenItemMatch not() {
        return ofType(NOT).setName("NEGATED");
    }

    public static TokenItemMatch hash() {
        return ofType(HASH).setName("HASH");
    }

    public static TokenPatternMatch rawBoolean() {
        return ofType(BOOLEAN).setEvaluator((p, d) -> p.flatten(false).equals("true")).setName("BOOLEAN");
    }

    public static TokenPatternMatch identifierC() {
        return ofType(IDENTIFIER_TYPE_C).setName("IDENTIFIER_C");
    }

    public static TokenPatternMatch identifierD() {
        return ofType(IDENTIFIER_TYPE_D).setName("IDENTIFIER_D");
    }

    public static TokenPatternMatch trailingString() {
        return ofType(TRAILING_STRING).setName("TRAILING_STRING");
    }

    public static SymbolVisibility parseVisibility(TokenPattern<?> pattern, SymbolVisibility defaultValue) {
        if(pattern == null) return defaultValue;
        switch(pattern.flatten(false)) {
            case "global": return SymbolVisibility.GLOBAL;
            case "public": return TridentSymbolVisibility.PUBLIC;
            case "local": return TridentSymbolVisibility.LOCAL;
            case "private": return TridentSymbolVisibility.PRIVATE;
            default: return defaultValue;
        }
    }

    public static TokenPatternMatch noToken() {
        return ofType(NO_TOKEN).setOptional();
    }

    public static TokenItemMatch glue() {
        return ofType(GLUE).setName("GLUE");
    }

    public static TokenItemMatch sameLine() {
        return ofType(LINE_GLUE).setName("LINE_GLUE");
    }

    public static TokenPatternMatch commandHeader(String text) {
        return matchItem(COMMAND_HEADER, text).addTags(TridentSuggestionTags.TAG_COMMAND);
    }

    public static TokenPatternMatch modifierHeader(String text) {
        return matchItem(MODIFIER_HEADER, text).addTags(TridentSuggestionTags.TAG_MODIFIER);
    }






    public static TokenStructureMatch string(PrismarineProductions productions) {
        return choice(
                ofType(STRING_LITERAL).setName("STRING_LITERAL").setEvaluator((p, d) -> BasicLiteralSet.parseQuotedString(p.flatten(false), p, (ISymbolContext) d[0])),
                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, String.class)
        ).setName("STRING");
    }

    public static TokenStructureMatch integer(PrismarineProductions productions) {
        return choice(ofType(INTEGER_NUMBER).setName("RAW_INTEGER").setEvaluator(BasicLiteralSet::evaluateIntegerPattern), PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, Integer.class)).setName("INTEGER");
    }

    public static TokenStructureMatch real(PrismarineProductions productions) {
        return choice(ofType(REAL_NUMBER).setName("RAW_REAL").setEvaluator((p, d) -> Double.parseDouble(p.flatten(false))), PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, Double.class)).setName("REAL");
    }

    public static TokenStructureMatch identifierA(PrismarineProductions productions) {
        return choice(string(productions), ofType(IDENTIFIER_TYPE_A).setName("RAW_IDENTIFIER_A").setEvaluator((p, d) -> p.flatten(false))).setName("IDENTIFIER_A");
    }

    public static TokenPatternMatch identifierA(String literal) {
        return matchItem(IDENTIFIER_TYPE_A, literal).setName("RAW_IDENTIFIER_A").setEvaluator((p, d) -> p.flatten(false));
    }

    public static TokenStructureMatch identifierB(PrismarineProductions productions) {
        return choice(
                ofType(IDENTIFIER_TYPE_B).setName("RAW_IDENTIFIER_B").setEvaluator((p, d) -> p.flatten(false)),
                wrapper(string(productions), (v, p, d) -> {
                    MinecraftLiteralSet.validateIdentifierB((String) v, p, (ISymbolContext) d[0]);
                    return v;
                })
        ).setName("IDENTIFIER_B");
    }

    public static TokenStructureMatch identifierBLimited(PrismarineProductions productions) {
        return choice(
                ofType(IDENTIFIER_TYPE_B_LIMITED).setName("RAW_IDENTIFIER_B").setEvaluator((p, d) -> p.flatten(false)),
                wrapper(string(productions), (v, p, d) -> {
                    MinecraftLiteralSet.validateIdentifierB((String) v, p, (ISymbolContext) d[0]);
                    return v;
                })
        ).setName("IDENTIFIER_B");
    }

    public static TokenItemMatch identifierX() {
        return ofType(IDENTIFIER_TYPE_X).setName("IDENTIFIER");
    }

    public static TokenPatternMatch versionLimited(PrismarineProjectWorker worker, String key, boolean defaultValue, TokenPatternMatch match) {
        return checkVersionFeature(worker.output.get(SetupModuleTask.INSTANCE), key, defaultValue) ? match : null;
    }

    public static boolean checkVersionFeature(CommandModule module, String key, boolean defaultValue) {
        VersionFeatures featureMap = VersionFeatureManager.getFeaturesForVersion(module.getSettingsManager().getTargetVersion());
        boolean available = defaultValue;
        if(featureMap != null) {
            available = featureMap.getBoolean(key, defaultValue);
        }
        return available;
    }

    public static boolean checkVersionFeature(PrismarineProjectWorker worker, String key, boolean defaultValue) {
        return checkVersionFeature(worker.output.get(SetupModuleTask.INSTANCE), key, defaultValue);
    }

    public static void uninstallCommands(PrismarineProductions productions) {
        if(productions.getPluginUnits() != null) {
            for(PrismarinePluginUnit unit : productions.getPluginUnits()) {
                ((TridentPluginUnitConfiguration.CustomCommandProduction)
                        unit.get(TridentPluginUnitConfiguration.CommandSyntaxFile.INSTANCE).getOutput()
                ).uninstallImportedCommand(productions.getOrCreateStructure("COMMAND"));
            }
        }
    }

    public static void registerPlugin(PrismarineProductions productions, PrismarinePlugin plugin) {
        productions.getOrCreateStructure("PLUGIN_NAME").add(literal(plugin.getName()));
    }

    public static void importPlugin(PrismarineProductions productions, String name, TokenPattern<?> p, Lexer lx) {
        boolean any = false;
        if(productions.getPluginUnits() != null) {
            for(PrismarinePluginUnit unit : productions.getPluginUnits()) {
                if(unit.getDefiningPlugin().getName().equals(name)) {
                    ((TridentPluginUnitConfiguration.CustomCommandProduction)
                            unit.get(TridentPluginUnitConfiguration.CommandSyntaxFile.INSTANCE).getOutput()
                    ).registerImportedCommand(productions.getOrCreateStructure("COMMAND"));
                    any = true;
                }
            }
        }
        if(!any && p != null && lx != null) {
            lx.getNotices().add(new Notice(NoticeType.WARNING, "Plugin '" + name + "' has no commands to import.", p));
        }
    }

    public static TokenPatternMatch nullPropagation() {
        return symbol("?").setName("NULL_PROPAGATION").setOptional().setRecessive().addTags(SuggestionTags.DISABLED);
    }
}
