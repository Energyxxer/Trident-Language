package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.commands.clear.ClearCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScorePlayersOperation;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreSet;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;

import java.util.ArrayList;

import static com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition.ConditionType.IF;

@AnalyzerMember(key = "broken")
public class BrokenScoreEvent implements ScoreEventCriteriaHandler {
    @Override
    public void startOnce(ItemEventFile itemEventFile) {
    }

    @Override
    public void start(ItemEventFile itemEventFile) {
        itemEventFile.getFunction().append(new ScoreSet(new LocalScore(new PlayerName("#CUSTOM_CONSUMED"), itemEventFile.getParent().getGlobalObjective()), 0));
    }

    @Override
    public void mid(ItemEventFile itemEventFile, ScoreEventCriteriaData data) {
        ScoreArgument scores = new ScoreArgument();
        scores.put(data.itemCriteriaObjective, new IntegerRange(1, null));

        Selector initialSelector = new Selector(Selector.BaseSelector.SENDER, scores);

        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        modifiers.add(new ExecuteConditionEntity(IF, initialSelector));

        if(data.customItem != null) {
            Objective objective = data.module.getObjectiveManager().create("btitem." + Integer.toString(data.customItem.getItemIdHash(),16), "dummy", new StringTextComponent("Temporary broken " + data.customItem));

            //store result score #OPERATION btitem.### clear @s <citem> 0
            data.function.append(new ExecuteCommand(new ClearCommand(new Selector(Selector.BaseSelector.SENDER),data.customItem.constructItem(NBTMode.TESTING), 0), new ExecuteStoreScore(new LocalScore(new PlayerName("#OPERATION"), objective))));

            //scoreboard players operation #OPERATION btitem.### -= @s btitem.###
            data.function.append(new ScorePlayersOperation(new LocalScore(new PlayerName("#OPERATION"), objective), ScorePlayersOperation.Operation.SUBTRACT, new LocalScore(new Selector(Selector.BaseSelector.SENDER), objective)));

            //if score #OPERATION btitem.### matches ..-1
            modifiers.add(new ExecuteConditionScoreMatch(IF, new LocalScore(new PlayerName("#OPERATION"), objective), new IntegerRange(null, -1)));

            data.function.append(new ExecuteCommand(
                    new ScoreSet(new LocalScore(new PlayerName("#CUSTOM_CONSUMED"), itemEventFile.getParent().getGlobalObjective()), 1),
                    modifiers));

            for(ItemEvent event : data.events) {
                ArrayList<ExecuteModifier> eventModifiers = new ArrayList<>(modifiers);
                if(event.modifiers != null) eventModifiers.addAll(event.modifiers);
                data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), eventModifiers));
            }

            data.function.append(new ExecuteCommand(new ClearCommand(new Selector(Selector.BaseSelector.SENDER),data.customItem.constructItem(NBTMode.TESTING), 0), new ExecuteStoreScore(new LocalScore(new Selector(Selector.BaseSelector.SENDER), objective))));

        } else {
            for(ItemEvent event : data.events) {
                ArrayList<ExecuteModifier> eventModifiers = new ArrayList<>(modifiers);
                if(event.pure) eventModifiers.add(new ExecuteConditionScoreMatch(IF, new LocalScore(new PlayerName("#CUSTOM_CONSUMED"), itemEventFile.getParent().getGlobalObjective()), new IntegerRange(0)));
                if(event.modifiers != null) eventModifiers.addAll(event.modifiers);
                data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), eventModifiers));
            }
        }
    }

    @Override
    public void end(ItemEventFile itemEventFile) {
    }

    @Override
    public void endOnce(ItemEventFile itemEventFile) {

    }
}
