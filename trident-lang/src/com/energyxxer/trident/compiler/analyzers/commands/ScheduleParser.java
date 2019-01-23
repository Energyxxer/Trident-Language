package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.schedule.ScheduleCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "schedule")
public class ScheduleParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        try {
            return new ScheduleCommand(CommonParsers.parseFunctionTag((TokenStructure) pattern.find("RESOURCE_LOCATION_TAGGED"), file), CommonParsers.parseTime(pattern.find("TIME"), file));
        } catch(IllegalArgumentException x) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), pattern));
            return null;
        }
    }
}
