package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagInt;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.NBTArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;

import java.util.ArrayList;

@ParserMember(key = "dropped")
public class DroppedScoreEvent implements ScoreEventCriteriaHandler {
    @Override
    public void start(ScoreEventCriteriaData data) {

    }

    @Override
    public void mid(ScoreEventCriteriaData data) {
        ScoreArgument scores = new ScoreArgument();
        scores.put(data.itemCriteriaObjective, new NumberRange<>(1, null));

        Selector initialSelector = new Selector(Selector.BaseSelector.SENDER, scores);

        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        modifiers.add(new ExecuteConditionEntity(ExecuteCondition.ConditionType.IF, new GenericEntity(initialSelector)));

        if(data.customItem != null) {
            modifiers.add(new ExecuteConditionEntity(ExecuteCondition.ConditionType.IF, new GenericEntity(new Selector(Selector.BaseSelector.ALL_ENTITIES, new TypeArgument(data.compiler.getModule().minecraft.types.entity.get("item")), new TagArgument("tdci_dropped"), new NBTArgument(new TagCompound(new TagCompound("Item", new TagCompound("tag", new TagInt("TridentCustomItem", data.customItem.getItemIdHash())))))))));
        }

        for(ItemEvent event : data.events) {
            data.function.append(new ExecuteCommand(new FunctionCommand(event.toCall), modifiers));
        }
    }

    @Override
    public void end(ScoreEventCriteriaData data) {

    }
}
