package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.weather.WeatherCommand;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.enumChoice;
import static com.energyxxer.prismarine.PrismarineProductions.group;

public class WeatherCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"weather"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("weather"),
                enumChoice(WeatherCommand.Mode.class).setName("WEATHER_MODE"),
                TridentProductions.integer(productions).setOptional().addTags("cspn:Duration").setName("DURATION")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        WeatherCommand.Mode mode = (WeatherCommand.Mode) pattern.find("WEATHER_MODE").evaluate();
        int duration = (int) pattern.findThenEvaluate("DURATION", WeatherCommand.DEFAULT_DURATION, ctx);

        try {
            return new WeatherCommand(mode, duration);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.tryFind("INTEGER"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
