package com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionDataEntity;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagShort;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.NBTArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFile;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;

import static com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition.ConditionType.IF;
import static com.energyxxer.commodore.functionlogic.commands.tag.TagCommand.Action.ADD;
import static com.energyxxer.commodore.functionlogic.commands.tag.TagCommand.Action.REMOVE;
import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.ALL_ENTITIES;
import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;

public class PrepareDroppedItemsFile extends SpecialFile {

    public PrepareDroppedItemsFile(SpecialFileManager parent) {
        super(parent, "prepare_dropped_items");
    }

    @Override
    public boolean shouldForceCompile() {
        return false;
    }

    @Override
    protected void compile() {
        function.append(new TagCommand(REMOVE, new Selector(ALL_ENTITIES, new TypeArgument(compiler.getModule().minecraft.types.entity.get("item"))), "tdci_dropped"));
        function.append(
                new ExecuteCommand(
                        new TagCommand(ADD, new Selector(SENDER), "tdci_dropped"),
                        new ExecuteAsEntity(new Selector(ALL_ENTITIES, new TypeArgument(compiler.getModule().minecraft.types.entity.get("item")), new NBTArgument(new TagCompound(new TagShort("Age", 0), new TagShort("PickupDelay", 40))))),
                        new ExecuteConditionDataEntity(IF, new Selector(SENDER), new NBTPath("Item", new NBTPath("tag", new NBTPath("TridentCustomItem")))),
                        new ExecuteConditionDataEntity(IF, new Selector(SENDER), new NBTPath("Thrower"))
                ));
    }
}
