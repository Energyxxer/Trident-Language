package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;

import java.util.ArrayList;

import static com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition.ConditionType.IF;

@AnalyzerMember(key="crafted")
public class CraftedScoreEvent implements ScoreEventCriteriaHandler {
    @Override
    public void startOnce(ItemEventFile itemEventFile) {

    }

    @Override
    public void start(ItemEventFile itemEventFile) {

    }

    @Override
    public void mid(ItemEventFile itemEventFile, ScoreEventCriteriaData data) {
        ScoreArgument scores = new ScoreArgument();
        scores.put(data.itemCriteriaObjective, new IntegerRange(1, null));

        Selector initialSelector = new Selector(Selector.BaseSelector.SENDER, scores);

        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        modifiers.add(new ExecuteConditionEntity(IF, initialSelector));

        for(ItemEvent event : data.events) {
            ArrayList<ExecuteModifier> eventModifiers = new ArrayList<>(modifiers);
            if(event.modifiers != null) eventModifiers.addAll(event.modifiers);
            data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), eventModifiers));
        }
    }

    @Override
    public void end(ItemEventFile itemEventFile) {

    }

    @Override
    public void endOnce(ItemEventFile itemEventFile) {

    }
}
