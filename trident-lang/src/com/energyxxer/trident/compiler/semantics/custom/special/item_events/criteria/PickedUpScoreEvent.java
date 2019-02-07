package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;

import java.util.ArrayList;

import static com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition.ConditionType.IF;

@AnalyzerMember(key = "picked_up")
public class PickedUpScoreEvent implements ScoreEventCriteriaHandler {
    @Override
    public void globalStart(SpecialFileManager mgr) {

    }

    @Override
    public void start(SpecialFileManager data, ItemEventFile itemEventFile) {

    }

    @Override
    public void mid(ScoreEventCriteriaData data) {
        ScoreArgument scores = new ScoreArgument();
        scores.put(data.itemCriteriaObjective, new NumberRange<>(1, null));

        Selector initialSelector = new Selector(Selector.BaseSelector.SENDER, scores);

        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        modifiers.add(new ExecuteConditionEntity(IF, initialSelector));

        if(data.customItem != null) {

        }

        for(ItemEvent event : data.events) {
            data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), modifiers));
        }
    }

    @Override
    public void end(SpecialFileManager data, ItemEventFile itemEventFile) {

    }

    @Override
    public void globalEnd(SpecialFileManager mgr) {

    }
}
