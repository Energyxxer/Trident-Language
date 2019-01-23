package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.trigger.TriggerCommand;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "trigger")
public class TriggerParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Objective objective = CommonParsers.parseObjective(pattern.find("OBJECTIVE_NAME"), file);
        if(!objective.getType().equals("trigger")) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unable to use objective '" + objective.getName() + "' with trigger; Expected objective of type 'trigger', instead got '" + objective.getType() + "'", pattern.find("OBJECTIVE_NAME")));
            return null;
        }
        TriggerCommand.Action action = TriggerCommand.Action.ADD;
        int amount = 1;
        TokenPattern<?> inner = pattern.find("INNER");
        if(inner != null) {
            action = inner.find("CHOICE").flatten(false).equals("set") ? TriggerCommand.Action.SET : TriggerCommand.Action.ADD;
            amount = CommonParsers.parseInt(inner.find("INTEGER"), file);
        }
        return new TriggerCommand(objective, action, amount);
    }
}
