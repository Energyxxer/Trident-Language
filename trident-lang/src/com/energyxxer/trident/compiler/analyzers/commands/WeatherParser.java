package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.weather.WeatherCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "weather")
public class WeatherParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        WeatherCommand.Mode mode = WeatherCommand.Mode.valueOf(pattern.find("CHOICE").flatten(false).toUpperCase());
        TokenPattern<?> rawDuration = pattern.find("INTEGER");
        return (rawDuration != null) ? new WeatherCommand(mode, CommonParsers.parseInt(rawDuration, file)) :
                new WeatherCommand(mode);
    }
}
