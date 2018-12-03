package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.weather.WeatherCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "weather")
public class WeatherParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        WeatherCommand.Mode mode = WeatherCommand.Mode.valueOf(pattern.find("CHOICE").flatten(false).toUpperCase());
        TokenPattern<?> rawDuration = pattern.find("INTEGER");
        return (rawDuration != null) ? new WeatherCommand(mode, Integer.parseInt(rawDuration.flatten(false))) :
                new WeatherCommand(mode);
    }
}
