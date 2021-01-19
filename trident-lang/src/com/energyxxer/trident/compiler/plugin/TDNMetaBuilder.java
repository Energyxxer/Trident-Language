package com.energyxxer.trident.compiler.plugin;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
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

    public TDNMetaBuilder(PrismarinePluginUnitConfiguration unitConfig, TokenPattern<?> filePattern, PrismarineProductions productions) {
        super(unitConfig, filePattern, productions);

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
