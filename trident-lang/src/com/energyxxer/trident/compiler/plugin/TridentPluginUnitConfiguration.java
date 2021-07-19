package com.energyxxer.trident.compiler.plugin;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.plugins.*;
import com.energyxxer.prismarine.plugins.syntax.PrismarineMetaBuilder;
import com.energyxxer.prismarine.plugins.syntax.PrismarineSyntaxFile;
import com.energyxxer.prismarine.util.PathMatcher;
import com.energyxxer.prismarine.walker.FileWalker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.tasks.SetupProductionsTask;
import com.energyxxer.trident.TridentFileUnitConfiguration;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.util.logger.Debug;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.trident.Trident.FUNCTION_EXTENSION;
import static com.energyxxer.trident.compiler.TridentProductions.resourceLocationFixer;

public class TridentPluginUnitConfiguration extends PrismarinePluginUnitConfiguration {

    public static final TridentPluginUnitConfiguration INSTANCE = new TridentPluginUnitConfiguration();

    private TridentPluginUnitConfiguration() {}

    @Override
    public String getStopPath() {
        return "(commands/(*)/)handler" + FUNCTION_EXTENSION;
    }

    @Override
    public void onStaticWalkerStop(PrismarinePluginUnit unit, File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarinePlugin> walker) throws IOException {
        unit.set(CommandName.INSTANCE, pathMatchResult.groups[2]);

        PrismarineSyntaxFile syntaxFile = unit.createSyntaxFile(Paths.get(pathMatchResult.groups[1] + "syntax.tdnmeta"));
        unit.set(CommandSyntaxFile.INSTANCE, syntaxFile);

        PrismarinePluginFile handlerFile = unit.createFile(relativePath);
        unit.set(CommandHandlerFile.INSTANCE, handlerFile);
    }

    @Override
    public void updateUnitForProjectWorker(PrismarinePluginUnit unit, PrismarineProjectWorker projectWorker) throws IOException {
        PrismarineSyntaxFile syntaxFile = unit.get(CommandSyntaxFile.INSTANCE);
        PrismarinePluginFile handlerFile = unit.get(CommandHandlerFile.INSTANCE);

        syntaxFile.update();
        handlerFile.update(TridentFileUnitConfiguration.INSTANCE);

        PrismarineProductions productions = projectWorker.output.get(SetupProductionsTask.INSTANCE).get(TridentFileUnitConfiguration.INSTANCE);

        syntaxFile.createSyntax(new TDNMetaBuilder(this, syntaxFile.getPattern(), productions));

        productions.registerPluginUnit(unit);

        //register namespaced command
        productions.getOrCreateStructure("COMMAND").add(
                ((CustomCommandProduction) syntaxFile.getOutput()).namespacedPattern
        );
    }

    @Override
    public TokenPatternMatch getStructureByName(String name, PrismarineProductions productions) {
        switch(name) {
            case "INNER_FUNCTION":
            case "ANONYMOUS_INNER_FUNCTION":
            case "OPTIONAL_NAME_INNER_FUNCTION":
            case "RESOURCE_LOCATION":
            case "RESOURCE_LOCATION_TAGGED":
            case "SELECTOR":
            case "ENTITY":
            case "LIMITED_ENTITY":
            case "TEXT_COMPONENT":
            case "TEXT_COLOR":
            case "NBT_COMPOUND":
            case "NBT_LIST":
            case "NBT_VALUE":
            case "NBT_PATH":
            case "INTEGER_NUMBER_RANGE":
            case "REAL_NUMBER_RANGE":
            case "COORDINATE_SET":
            case "TWO_COORDINATE_SET":
            case "ROTATION":
            case "BLOCK_ID":
            case "ITEM_ID":
            case "ENTITY_ID":
            case "ENTITY_ID_TAGGED":
            case "EFFECT_ID":
            case "PARTICLE_ID":
            case "ENCHANTMENT_ID":
            case "DIMENSION_ID":
            case "ATTRIBUTE_ID":
            case "BIOME_ID":
            case "SLOT_ID":
            case "PARTICLE":
            case "BLOCK":
            case "BLOCK_TAGGED":
            case "ITEM":
            case "ITEM_TAGGED":
            case "UUID":
            case "POINTER":
            case "DICTIONARY":
            case "LIST":
            case "INTERPOLATION_BLOCK":
            case "INTERPOLATION_VALUE":
            case "MODIFIER":
            case "STRING_LITERAL_OR_IDENTIFIER_A":
            case "OBJECTIVE_NAME":
            case "SCORE":
            case "SCORE_OPTIONAL_OBJECTIVE":
            case "NEW_ENTITY_LITERAL":
            case "ANCHOR":
            case "LINE_SAFE_INTERPOLATION_VALUE":
                return productions.getOrCreateStructure(name);

            case "GAMEMODE_ID":
            case "GAMEMODE": return productions.getOrCreateStructure("GAMEMODE_ID");
            case "GAMERULE_ID":
            case "GAMERULE": return productions.getOrCreateStructure("GAMERULE_ID");
            case "STRUCTURE_ID":
            case "STRUCTURE": return productions.getOrCreateStructure("STRUCTURE_ID");
            case "DIFFICULTY_ID":
            case "DIFFICULTY": return productions.getOrCreateStructure("DIFFICULTY_ID");
            case "NUMERIC_DATA_TYPE":
            case "NUMERIC_NBT_TYPE": return productions.getOrCreateStructure("NUMERIC_NBT_TYPE");
            case "INTEGER": return TridentProductions.integer(productions);
            case "BOOLEAN": return TridentProductions.rawBoolean();
            case "STRING": return TridentProductions.string(productions);
            case "REAL": return TridentProductions.real(productions);
            case "COLON": return TridentProductions.colon();
            case "COMMA": return TridentProductions.comma();
            case "DOT": return TridentProductions.dot();
            case "EQUALS": return TridentProductions.equals();
            case "CARET": return TridentProductions.caret();
            case "TILDE": return TridentProductions.tilde();
            case "NOT": return TridentProductions.not();
            case "HASH": return TridentProductions.hash();
            case "GLUE": return TridentProductions.glue();
            case "SAME_LINE": return TridentProductions.sameLine();
            case "IDENTIFIER_A": return TridentProductions.identifierA(productions);
            case "IDENTIFIER_B": return TridentProductions.identifierB(productions);
            case "IDENTIFIER_B_LIMITED": return TridentProductions.identifierBLimited(productions);
            case "IDENTIFIER_C": return TridentProductions.identifierC();
            case "IDENTIFIER_D": return TridentProductions.identifierD();
            case "IDENTIFIER_X":
            case "IDENTIFIER_Y":
                return TridentProductions.identifierX();
            case "TRAILING_STRING": return TridentProductions.trailingString();
            default: return null;
        }
    }

    public static class CustomCommandProduction extends TokenPatternMatch {
        String pluginName;
        String commandHeader;
        TokenPatternMatch pattern;

        private final TokenPatternMatch namespacedPattern;
        private final TokenPatternMatch importedPattern;

        public CustomCommandProduction(String pluginName, String commandHeader, TokenPatternMatch pattern) {
            this.pluginName = pluginName;
            this.commandHeader = commandHeader;
            this.pattern = pattern;

            this.namespacedPattern = group(
                    resourceLocationFixer,
                    TridentProductions.commandHeader(pluginName + ":" + commandHeader).setName("CUSTOM_COMMAND_HEADER"),
                    pattern
            )
                    .setSimplificationFunctionContentIndex(1)
                    .addTags(PrismarineMetaBuilder.PLUGIN_CREATED_TAG);
            this.importedPattern = group(
                    TridentProductions.commandHeader(commandHeader).setName("CUSTOM_COMMAND_HEADER"),
                    pattern
            )
                    .setSimplificationFunctionContentIndex(1)
                    .addTags(PrismarineMetaBuilder.PLUGIN_CREATED_TAG);
        }

        public void registerNamespacedCommand(TokenStructureMatch COMMAND) {
            COMMAND.add(namespacedPattern);
        }

        public void registerImportedCommand(TokenStructureMatch COMMAND) {
            COMMAND.addDynamic(importedPattern);
        }

        public void uninstallImportedCommand(TokenStructureMatch COMMAND) {
            COMMAND.removeDynamic(importedPattern);
        }


        @Override
        public String deepToString(int i) {
            return namespacedPattern.deepToString(i);
        }

        @Override
        public String toTrimmedString() {
            return namespacedPattern.toTrimmedString();
        }

        @Override
        public TokenMatchResponse match(int i, Lexer lexer) {
            throw new UnsupportedOperationException();
        }
    }

    //Data identifiers
    static class CommandName extends PluginDataIdentifier<String> {
        public static final CommandName INSTANCE = new CommandName();
        private CommandName() {}
    }
    public static class CommandSyntaxFile extends PluginDataIdentifier<PrismarineSyntaxFile> {
        public static final CommandSyntaxFile INSTANCE = new CommandSyntaxFile();
        private CommandSyntaxFile() {}
    }
    static class CommandHandlerFile extends PluginDataIdentifier<PrismarinePluginFile> {
        public static final CommandHandlerFile INSTANCE = new CommandHandlerFile();
        private CommandHandlerFile() {}
    }
}
