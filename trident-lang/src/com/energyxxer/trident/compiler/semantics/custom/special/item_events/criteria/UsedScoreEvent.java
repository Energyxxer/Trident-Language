package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.PrepareHeldItemsFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.SaveHeldItemsFile;

import java.util.ArrayList;

import static com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition.ConditionType.IF;
import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;

@AnalyzerMember(key = "used")
public class UsedScoreEvent implements ScoreEventCriteriaHandler {
    @Override
    public void startOnce(ItemEventFile itemEventFile) {

    }

    @Override
    public void start(ItemEventFile itemEventFile) {
        ((PrepareHeldItemsFile) itemEventFile.getParent().get("prepare_held_items")).setObjectives(itemEventFile.getObjectives());
        itemEventFile.getParent().get("prepare_held_items").startCompilation();
        itemEventFile.getFunction().append(new FunctionCommand(itemEventFile.getParent().get("prepare_held_items").getFunction()));
    }

    @Override
    public void mid(ItemEventFile itemEventFile, ScoreEventCriteriaData data) {
        ScoreArgument scores = new ScoreArgument();
        scores.put(data.itemCriteriaObjective, new NumberRange<>(1, null));

        Selector initialSelector = new Selector(Selector.BaseSelector.SENDER, scores);

        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        modifiers.add(new ExecuteConditionEntity(IF, initialSelector));

        if(data.customItem != null) {
            scores.put(itemEventFile.getObjectives().oldHeld, new NumberRange<>(data.customItem.getItemIdHash()));
        }

        for(ItemEvent event : data.events) {
            ArrayList<ExecuteModifier> eventModifiers = new ArrayList<>(modifiers);
            if(data.customItem == null && event.pure) eventModifiers.add(new ExecuteConditionScoreMatch(ExecuteCondition.ConditionType.IF, new LocalScore(new Selector(SENDER), itemEventFile.getObjectives().oldHeld), new NumberRange<>(0)));
            data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), eventModifiers));
        }
    }

    @Override
    public void end(ItemEventFile itemEventFile) {
        ((SaveHeldItemsFile) itemEventFile.getParent().get("save_held_items")).setObjectives(itemEventFile.getObjectives());
        itemEventFile.getParent().get("save_held_items").startCompilation();
        itemEventFile.getFunction().append(new FunctionCommand(itemEventFile.getParent().get("save_held_items").getFunction()));
    }

    @Override
    public void endOnce(ItemEventFile itemEventFile) {

    }
}
