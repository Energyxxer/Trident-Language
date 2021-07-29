package com.energyxxer.trident.compiler.plugin;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.plugins.PrismarinePluginUnit;
import com.energyxxer.prismarine.plugins.PrismarinePluginUnitConfiguration;
import com.energyxxer.prismarine.plugins.syntax.PrismarineMetaBuilder;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import java.util.Collections;
import java.util.List;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.NO_TOKEN;

public class TDNMetaBuilder extends PrismarineMetaBuilder {

    public static final String STORE_VAR_TAG_PREFIX = "__PLUGIN_STORE_VAR:";
    public static final String STORE_FLAT_TAG_PREFIX = "__PLUGIN_STORE_FLAT:";
    public static final String STORE_METADATA_TAG_PREFIX = "__PLUGIN_STORE_VAR_METADATA:";

    public TDNMetaBuilder(PrismarinePluginUnitConfiguration unitConfig, TokenPattern<?> filePattern, PrismarineProductions productions) {
        super(unitConfig, filePattern, productions);

        registerFunction("storeVar", TokenMatchValue.class, (value, args) -> {
            if(args.size() >= 1) {
                if(args.get(0) instanceof StringLiteralValue) {
                    if(!((TokenMatchValue) value).patternMatch.hasTag(PLUGIN_CREATED_TAG)) {
                        TokenGroupMatch match = new TokenGroupMatch().append(((TokenMatchValue) value).patternMatch);
                        match.addTag(PLUGIN_CREATED_TAG).addTag(STORE_VAR_TAG_PREFIX + ((StringLiteralValue) args.get(0)).stringValue);
                        for(int i = 1; i < args.size(); i++) {
                            Value arg = args.get(i);
                            if(arg instanceof StringLiteralValue) {
                                match.addTag(STORE_METADATA_TAG_PREFIX + ((StringLiteralValue) arg).stringValue);
                            } else {
                                throw new IllegalArgumentException("Function 'storeVar' only accepts String Literal values at arguments 1 and later, found " + arg.getClass().getSimpleName());
                            }
                        }
                        return new TokenMatchValue(match);
                    } else {
                        throw new IllegalArgumentException("Function 'storeVar' can only be performed on native patterns");
                    }
                } else {
                    throw new IllegalArgumentException("Function 'storeVar' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'storeVar' requires at least 1 parameter, found " + args.size());
            }
        });
        registerFunction("storeFlat", TokenMatchValue.class, (value, args) -> {
            if(args.size() >= 1) {
                String delimiter = " ";
                if(args.size() >= 2) {
                    if(args.get(1) instanceof StringLiteralValue) {
                        delimiter = ((StringLiteralValue) args.get(1)).stringValue;
                    } else {
                        throw new IllegalArgumentException("Function 'storeFlat' only accepts String Literal values at argument 1, found " + args.get(1).getClass().getSimpleName());
                    }
                }
                if(args.get(0) instanceof StringLiteralValue) {
                    return new TokenMatchValue(new TokenGroupMatch().append(((TokenMatchValue) value).patternMatch).addTags(PLUGIN_CREATED_TAG).addTags(STORE_FLAT_TAG_PREFIX + delimiter).addTags(STORE_VAR_TAG_PREFIX + ((StringLiteralValue) args.get(0)).stringValue));
                } else {
                    throw new IllegalArgumentException("Function 'storeFlat' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'storeFlat' requires at least 1 parameter, found " + args.size());
            }
        });

        registerFunction("noToken", (ignore, args) -> {
            TokenItemMatch nt = new TokenItemMatch(NO_TOKEN);
            nt.addTags(PLUGIN_CREATED_TAG);
            nt.setOptional();
            return new TokenMatchValue(nt);
        });
        registerFunction("brace", (ignore, args) -> {
            if(args.size() >= 1) {
                if(args.get(0) instanceof StringLiteralValue) {
                    String text = ((StringLiteralValue) args.get(0)).stringValue;
                    return new TokenMatchValue(new TokenItemMatch(TridentTokens.BRACE, text).addTags(PLUGIN_CREATED_TAG));
                } else {
                    throw new IllegalArgumentException("Function 'brace' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'brace' requires at least 1 parameter, found " + args.size());
            }
        });
    }

    @Override
    public void build(PrismarinePluginUnit unit) {
        super.build(unit);

        if(!returnValue.hasTag(PLUGIN_CREATED_TAG)) {
            returnValue = new TokenGroupMatch().append(returnValue).addTags(PLUGIN_CREATED_TAG);
        }

        returnValue.setEvaluator((pattern, data) -> {
            ISymbolContext ctx = (ISymbolContext) data[0];
            TridentFile writingFile = ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile();
            new PluginCommandParser().handleCommand(unit, pattern, (List<ExecuteModifier>) data[1], ctx, writingFile.getFunction());
            return Collections.emptyList();
        });

        String pluginName = unit.getDefiningPlugin().getName();
        String commandHeader = unit.get(TridentPluginUnitConfiguration.CommandName.INSTANCE);

        returnValue = new TridentPluginUnitConfiguration.CustomCommandProduction(pluginName, commandHeader, returnValue);
    }
}
