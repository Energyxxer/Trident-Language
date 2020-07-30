package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.schedule.ScheduleClearCommand;
import com.energyxxer.commodore.functionlogic.commands.schedule.ScheduleCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "schedule")
public class ScheduleParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();

        switch(inner.getName()) {
            case "SCHEDULE_FUNCTION": {
                try {
                    ScheduleCommand.ScheduleMode mode = ScheduleCommand.ScheduleMode.REPLACE;
                    if(inner.find("SCHEDULE_MODE") != null && inner.find("SCHEDULE_MODE").flatten(false).equals("append")) {
                        mode = ScheduleCommand.ScheduleMode.APPEND;
                    }
                    return new ScheduleCommand(CommonParsers.parseFunctionTag((TokenStructure) inner.find("FUNCTION_REFERENCE.RESOURCE_LOCATION_TAGGED"), ctx), CommonParsers.parseTime(inner.find("TIME"), ctx), mode);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, inner, ctx)
                            .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, inner.tryFind("TIME"))
                            .invokeThrow();
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
                }
            }
            case "SCHEDULE_CLEAR": {
                return new ScheduleClearCommand(CommonParsers.parseFunctionTag((TokenStructure) inner.find("FUNCTION_REFERENCE.RESOURCE_LOCATION_TAGGED"), ctx));
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}
