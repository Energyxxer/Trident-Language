package com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation;

import com.energyxxer.commodore.functionlogic.commands.data.DataGetCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScorePlayersOperation;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreSet;
import com.energyxxer.commodore.functionlogic.nbt.TagByte;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTListMatch;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathKey;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFile;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventObjectives;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;

public class PrepareHeldItemsFile extends SpecialFile {

    private ItemEventObjectives objectives;

    public PrepareHeldItemsFile(SpecialFileManager parent) {
        super(parent, "prepare_held_items");
    }

    @Override
    public boolean shouldForceCompile() {
        return false;
    }

    @Override
    protected void compile() {
        Selector sender = new Selector(SENDER);
        NBTPath mainhandCI = new NBTPath("SelectedItem", new NBTPath("tag", new NBTPath("TridentCustomItem"))); //SelectedItem.tag.TridentCustomItem
        NBTPath offhandCI = new NBTPath(new NBTPathKey("Inventory"), new NBTListMatch(new TagCompound(new TagByte("Slot", -106))), new NBTPathKey("tag"), new NBTPathKey("TridentCustomItem")); //Inventory[{Slot:-106b}].tag.TridentCustomItem

        function.append(new ScoreSet(new LocalScore(sender, objectives.mainhand), 0)); //scoreboard players set @s tdci_mainhand 0
        function.append(new ScoreSet(new LocalScore(sender, objectives.offhand), 0)); //scoreboard players set @s tdci_offhand 0
        function.append(new ExecuteCommand(new DataGetCommand(sender, mainhandCI), new ExecuteConditionDataEntity(ExecuteCondition.ConditionType.IF, sender, mainhandCI), new ExecuteStoreScore(ExecuteStore.StoreValue.RESULT, new LocalScore(sender, objectives.mainhand)))); //execute if data entity @s SelectedItem.tag.TridentCustomItem store result score @s tdci_mainhand run data get entity @s SelectedItem.tag.TridentCustomItem
        function.append(new ExecuteCommand(new DataGetCommand(sender, offhandCI), new ExecuteConditionDataEntity(ExecuteCondition.ConditionType.IF, sender, offhandCI), new ExecuteStoreScore(ExecuteStore.StoreValue.RESULT, new LocalScore(sender, objectives.offhand)))); //execute if data entity @s Inventory[{Slot:-106b}].tag.TridentCustomItem store result score @s tdci_offhand run data get entity @s Inventory[{Slot:-106b}].tag.TridentCustomItem
        function.append(new ScorePlayersOperation(new LocalScore(sender, objectives.held), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, objectives.mainhand))); //scoreboard players operation @s tdci_held = @s tdci_mainhand
        function.append(new ExecuteCommand(new ScorePlayersOperation(new LocalScore(sender, objectives.held), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, objectives.offhand)), new ExecuteConditionScoreMatch(ExecuteCondition.ConditionType.IF, new LocalScore(sender, objectives.held), new IntegerRange(0)))); //execute if score @s tdci_held matches 1 run scoreboard players operation @s tdci_held = @s tdci_offhand
    }

    public void setObjectives(ItemEventObjectives objectives) {
        this.objectives = objectives;
    }
}
