package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.time.TimeAddCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeSetCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "time")
public class TimeCommand implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "QUERY": {
                return new TimeQueryCommand(TimeQueryCommand.TimeCounter.valueOf(inner.find("CHOICE").flatten(false).toUpperCase()));
            }
            case "ADD": {
                return new TimeAddCommand(CommonParsers.parseTime(inner.find("TIME"), file));
            }
            case "SET": {
                TokenPattern<?> sub = ((TokenStructure) inner.find("CHOICE")).getContents();
                switch(sub.getName()) {
                    case "CHOICE": {
                        return new TimeSetCommand(TimeSetCommand.TimeOfDay.valueOf(sub.flatten(false).toUpperCase()));
                    }
                    case "TIME": {
                        return new TimeSetCommand(CommonParsers.parseTime(sub, file));
                    }
                    default: {
                        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + sub.getName() + "'", sub));
                        return null;
                    }
                }
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }
}
