package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugReportCommand;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugStartCommand;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugStopCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "debug")
public class DebugParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(pattern.find("CHOICE").flatten(false)) {
            case "report": return new DebugReportCommand();
            case "start": return new DebugStartCommand();
            case "stop": return new DebugStopCommand();
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.find("CHOICE").flatten(false) + "'", pattern, ctx);
            }
        }
    }
}
