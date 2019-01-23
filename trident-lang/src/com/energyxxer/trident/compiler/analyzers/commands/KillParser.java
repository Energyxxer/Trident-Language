package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.kill.KillCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;

@AnalyzerMember(key = "kill")
public class KillParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity entity = EntityParser.parseEntity(pattern.find(".ENTITY"), file);
        if(entity == null) entity = new Selector(SENDER);
        return new KillCommand(entity);
    }
}
