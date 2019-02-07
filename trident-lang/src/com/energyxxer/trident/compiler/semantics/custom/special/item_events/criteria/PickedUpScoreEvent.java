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
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;

import java.util.ArrayList;

import static com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition.ConditionType.IF;

@AnalyzerMember(key = "picked_up")
public class PickedUpScoreEvent implements ScoreEventCriteriaHandler {
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
        scores.put(data.itemCriteriaObjective, new NumberRange<>(1, null));

        Selector initialSelector = new Selector(Selector.BaseSelector.SENDER, scores);

        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        modifiers.add(new ExecuteConditionEntity(IF, initialSelector));

        if(data.customItem != null) {
            Objective objective = data.compiler.getModule().getObjectiveManager().create("ptitem." + Integer.toString(data.customItem.getItemIdHash(),16), "dummy", new StringTextComponent("Temporary picked_up " + data.customItem), true);

            //store result score #OPERATION ptitem.### clear @s <citem> 0
            data.function.append(new ExecuteCommand(new ClearCommand(new Selector(Selector.BaseSelector.SENDER),data.customItem.constructItem(NBTMode.TESTING), 0), new ExecuteStoreScore(new LocalScore(new PlayerName("#OPERATION"), objective))));

            //scoreboard players operation #OPERATION ptitem.### -= @s ptitem.###
            data.function.append(new ScorePlayersOperation(new LocalScore(new PlayerName("#OPERATION"), objective), ScorePlayersOperation.Operation.SUBTRACT, new LocalScore(new Selector(Selector.BaseSelector.SENDER), objective)));

            //if score #OPERATION ptitem.### matches 1..
            modifiers.add(new ExecuteConditionScoreMatch(IF, new LocalScore(new PlayerName("#OPERATION"), objective), new NumberRange<>(1, null)));

            data.function.append(new ExecuteCommand(
                    new ScoreSet(new LocalScore(new PlayerName("#CUSTOM_CONSUMED"), itemEventFile.getParent().getGlobalObjective()), 1),
                    modifiers));

            for(ItemEvent event : data.events) {
                data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), modifiers));
            }

            data.function.append(new ExecuteCommand(new ClearCommand(new Selector(Selector.BaseSelector.SENDER),data.customItem.constructItem(NBTMode.TESTING), 0), new ExecuteStoreScore(new LocalScore(new Selector(Selector.BaseSelector.SENDER), objective))));

        } else {
            for(ItemEvent event : data.events) {
                ArrayList<ExecuteModifier> innerModifiers = new ArrayList<>(modifiers);
                if(event.pure) innerModifiers.add(new ExecuteConditionScoreMatch(IF, new LocalScore(new PlayerName("#CUSTOM_CONSUMED"), itemEventFile.getParent().getGlobalObjective()), new NumberRange<>(0)));
                data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), innerModifiers));
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
