package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.trigger.TriggerCommand;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "trigger")
public class TriggerParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        Objective objective = CommonParsers.parseObjective(pattern.find("OBJECTIVE_NAME"), ctx);
        TriggerCommand.Action action = TriggerCommand.Action.ADD;
        int amount = 1;
        TokenPattern<?> inner = pattern.find("INNER");
        if(inner != null) {
            action = inner.find("CHOICE").flatten(false).equals("set") ? TriggerCommand.Action.SET : TriggerCommand.Action.ADD;
            amount = CommonParsers.parseInt(inner.find("INTEGER"), ctx);
        }
        try {
            return new TriggerCommand(objective, action, amount);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("OBJECTIVE_NAME"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
        }
    }
}
