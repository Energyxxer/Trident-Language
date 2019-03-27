package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionScoreMatch;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreSet;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagInt;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.NBTArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;

import java.util.ArrayList;

import static com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition.ConditionType.IF;
import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.ALL_ENTITIES;

@AnalyzerMember(key = "dropped")
public class DroppedScoreEvent implements ScoreEventCriteriaHandler {
    @Override
    public void startOnce(ItemEventFile itemEventFile) {
        itemEventFile.getParent().get("prepare_dropped_items").startCompilation();
        itemEventFile.getParent().getTickFunction().append(new FunctionCommand(itemEventFile.getParent().get("prepare_dropped_items").getFunction()));
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
            modifiers.add(new ExecuteConditionEntity(IF, new Selector(ALL_ENTITIES, new TypeArgument(data.compiler.getModule().minecraft.types.entity.get("item")), new TagArgument("tdci_dropped"), new NBTArgument(new TagCompound(new TagCompound("Item", new TagCompound("tag", new TagInt("TridentCustomItem", data.customItem.getItemIdHash()))))))));

            data.function.append(new ExecuteCommand(
                    new ScoreSet(new LocalScore(new PlayerName("#CUSTOM_CONSUMED"), itemEventFile.getParent().getGlobalObjective()), 1),
                    modifiers));
        }

        for(ItemEvent event : data.events) {
            ArrayList<ExecuteModifier> eventModifiers = new ArrayList<>(modifiers);
            if(event.pure) eventModifiers.add(new ExecuteConditionScoreMatch(IF, new LocalScore(new PlayerName("#CUSTOM_CONSUMED"), itemEventFile.getParent().getGlobalObjective()), new IntegerRange(0)));
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
