package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.commands.clear.ClearCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScorePlayersOperation;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
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
            Objective objective = data.compiler.getModule().getObjectiveManager().create("ptitem." + data.customItem.getItemIdHash(), "dummy", new StringTextComponent("Temporary picked_up " + data.customItem), true);

            //store result score #OPERATION ptitem.### clear @s <citem> 0
            data.function.append(new ExecuteCommand(new ClearCommand(new Selector(Selector.BaseSelector.SENDER),data.customItem.constructItem(NBTMode.TESTING), 0), new ExecuteStoreScore(new LocalScore(new PlayerName("#OPERATION"), objective))));

            //scoreboard players operation #OPERATION ptitem.### -= @s ptitem.###
            data.function.append(new ScorePlayersOperation(new LocalScore(new PlayerName("#OPERATION"), objective), ScorePlayersOperation.Operation.SUBTRACT, new LocalScore(new Selector(Selector.BaseSelector.SENDER), objective)));

            modifiers.add(new ExecuteConditionScoreMatch(IF, new LocalScore(new PlayerName("#OPERATION"), objective), new NumberRange<>(1, null)));

            for(ItemEvent event : data.events) {
                data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), modifiers));
            }

            data.function.append(new ExecuteCommand(new ClearCommand(new Selector(Selector.BaseSelector.SENDER),data.customItem.constructItem(NBTMode.TESTING), 0), new ExecuteStoreScore(new LocalScore(new Selector(Selector.BaseSelector.SENDER), objective))));

        } else {
            for(ItemEvent event : data.events) {
                data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), modifiers));
            }
        }
    }

    @Override
    public void end(SpecialFileManager data, ItemEventFile itemEventFile) {

    }

    @Override
    public void globalEnd(SpecialFileManager mgr) {

    }
}
