package com.energyxxer.trident.compiler.semantics.custom.special.item_events;

import com.energyxxer.commodore.functionlogic.commands.data.DataGetCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScorePlayersOperation;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreReset;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreSet;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.nbt.TagByte;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagShort;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTListMatch;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathKey;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.NBTArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFile;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria.ScoreEventCriteriaData;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria.ScoreEventCriteriaHandler;

import java.util.ArrayList;
import java.util.HashMap;

public class ItemEventFile extends SpecialFile {
    private final TridentCompiler compiler;
    private final SpecialFileManager parent;
    private final HashMap<ItemEventCollection.ItemScoreEventType, HashMap<Type, HashMap<CustomItem, ArrayList<FunctionReference>>>> definedItems = new HashMap<>();

    private Function function;

    public ItemEventFile(SpecialFileManager parent) {
        this.compiler = parent.getCompiler();
        this.parent = parent;
    }

    public void addCustomItem(ItemEventCollection.ItemScoreEventType eventType, Type itemType, CustomItem customItem, FunctionReference function) {
        if(!definedItems.containsKey(eventType)) definedItems.put(eventType, new HashMap<>());
        if(!definedItems.get(eventType).containsKey(itemType)) {
            definedItems.get(eventType).put(itemType, new HashMap<>());
            definedItems.get(eventType).get(itemType).put(null, new ArrayList<>());
        }
        if(!definedItems.get(eventType).get(itemType).containsKey(customItem)) definedItems.get(eventType).get(itemType).put(customItem, new ArrayList<>());
        definedItems.get(eventType).get(itemType).get(customItem).add(function);
    }

    public void compile() {
        if(definedItems.isEmpty()) return;

        {
            Function prepareDroppedItems = parent.getNamespace().functions.create("trident/prepare_dropped_items");
            prepareDroppedItems.append(new TagCommand(TagCommand.Action.REMOVE, new GenericEntity(new Selector(Selector.BaseSelector.ALL_ENTITIES, new TypeArgument(compiler.getModule().minecraft.types.entity.get("item")))), "tdci_dropped"));
            prepareDroppedItems.append(
                    new ExecuteCommand(
                            new TagCommand(TagCommand.Action.ADD, new GenericEntity(new Selector(Selector.BaseSelector.SENDER)), "tdci_dropped"),
                            new ExecuteAsEntity(new GenericEntity(new Selector(Selector.BaseSelector.ALL_ENTITIES, new TypeArgument(compiler.getModule().minecraft.types.entity.get("item")), new NBTArgument(new TagCompound(new TagShort("Age",0), new TagShort("PickupDelay",40)))))),
                            new ExecuteConditionDataEntity(ExecuteCondition.ConditionType.IF, new GenericEntity(new Selector(Selector.BaseSelector.SENDER)), new NBTPath("Item", new NBTPath("tag", new NBTPath("TridentCustomItem")))),
                            new ExecuteConditionDataEntity(ExecuteCondition.ConditionType.IF, new GenericEntity(new Selector(Selector.BaseSelector.SENDER)), new NBTPath("Thrower"))
                    ));
            parent.getTickFunction().append(new FunctionCommand(prepareDroppedItems));
        }


        function = compiler.getModule().createNamespace(compiler.getDefaultNamespace()).functions.create("trident/item_events");
        parent.getTickFunction().append(new ExecuteCommand(new FunctionCommand(function), new ExecuteAsEntity(new GenericEntity(new Selector(Selector.BaseSelector.ALL_PLAYERS))), new ExecuteAtEntity(new GenericEntity(new Selector(Selector.BaseSelector.SENDER)))));

        Objective mainhand = compiler.getModule().getObjectiveManager().create("tdci_mainhand", true);
        Objective offhand = compiler.getModule().getObjectiveManager().create("tdci_offhand", true);
        Objective held = compiler.getModule().getObjectiveManager().create("tdci_held", true);
        Objective oldMainhand = compiler.getModule().getObjectiveManager().create("oldtdci_mainhand", true);
        Objective oldOffhand = compiler.getModule().getObjectiveManager().create("oldtdci_offhand", true);
        Objective oldHeld = compiler.getModule().getObjectiveManager().create("oldtdci_held", true);

        Function prepareHeldItems;
        {
            prepareHeldItems = parent.getNamespace().functions.create("trident/prepare_held_items");
            GenericEntity sender = new GenericEntity(new Selector(Selector.BaseSelector.SENDER));
            NBTPath mainhandCI = new NBTPath("SelectedItem", new NBTPath("tag", new NBTPath("TridentCustomItem"))); //SelectedItem.tag.TridentCustomItem
            NBTPath offhandCI = new NBTPath(new NBTPathKey("Inventory"), new NBTListMatch(new TagCompound(new TagByte("Slot", -106))), new NBTPathKey("tag"), new NBTPathKey("TridentCustomItem")); //Inventory[{Slot:-106b}].tag.TridentCustomItem

            prepareHeldItems.append(new ScoreSet(new LocalScore(sender, mainhand), 0)); //scoreboard players set @s tdci_mainhand 0
            prepareHeldItems.append(new ScoreSet(new LocalScore(sender, offhand), 0)); //scoreboard players set @s tdci_offhand 0
            prepareHeldItems.append(new ExecuteCommand(new DataGetCommand(sender, mainhandCI), new ExecuteConditionDataEntity(ExecuteCondition.ConditionType.IF, sender, mainhandCI), new ExecuteStoreScore(ExecuteStore.StoreValue.RESULT, new LocalScore(sender, mainhand)))); //execute if data entity @s SelectedItem.tag.TridentCustomItem store result score @s tdci_mainhand run data get entity @s SelectedItem.tag.TridentCustomItem
            prepareHeldItems.append(new ExecuteCommand(new DataGetCommand(sender, offhandCI), new ExecuteConditionDataEntity(ExecuteCondition.ConditionType.IF, sender, offhandCI), new ExecuteStoreScore(ExecuteStore.StoreValue.RESULT, new LocalScore(sender, offhand)))); //execute if data entity @s Inventory[{Slot:-106b}].tag.TridentCustomItem store result score @s tdci_offhand run data get entity @s Inventory[{Slot:-106b}].tag.TridentCustomItem
            prepareHeldItems.append(new ScorePlayersOperation(new LocalScore(sender, held), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, mainhand))); //scoreboard players operation @s tdci_held = @s tdci_mainhand
            prepareHeldItems.append(new ExecuteCommand(new ScorePlayersOperation(new LocalScore(sender, held), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, offhand)), new ExecuteConditionScoreMatch(ExecuteCondition.ConditionType.IF, new LocalScore(sender, held), new NumberRange<>(0)))); //execute if score @s tdci_held matches 1 run scoreboard players operation @s tdci_held = @s tdci_offhand
        }

        function.append(new FunctionCommand(prepareHeldItems));

        for(var eventEntry : definedItems.entrySet()) {
            ItemEventCollection.ItemScoreEventType eventType = eventEntry.getKey();
            for(var typeEntry : eventEntry.getValue().entrySet()) {
                Type itemType = typeEntry.getKey();

                String criteria = "minecraft." + eventType.name().toLowerCase() + ":" + itemType.toString().replace(':','.');
                Objective objective = compiler.getModule().getObjectiveManager().create(eventType.name().toLowerCase().charAt(0) + "item." + new TridentUtil.ResourceLocation(itemType.toString()).body.hashCode(), criteria, new StringTextComponent(eventType.name().toLowerCase() + " item " + itemType), true);

                ScoreArgument scores = new ScoreArgument();
                scores.put(objective, new NumberRange<>(1, null));

                for(var itemEntry : typeEntry.getValue().entrySet()) {
                    ScoreEventCriteriaData data = new ScoreEventCriteriaData(compiler, itemType, objective, function, itemEntry.getKey(), itemEntry.getValue(), mainhand, offhand, held, oldMainhand, oldOffhand, oldHeld);
                    ParserManager.getParser(ScoreEventCriteriaHandler.class, eventType.name().toLowerCase()).mid(data);
                }

                function.append(new ScoreReset(new GenericEntity(new Selector(Selector.BaseSelector.SENDER, scores)), objective));
            }
        }

        Function saveHeldItems;
        {
            saveHeldItems = compiler.getModule().createNamespace(compiler.getDefaultNamespace()).functions.create("trident/save_held_items");
            GenericEntity sender = new GenericEntity(new Selector(Selector.BaseSelector.SENDER));

            saveHeldItems.append(new ScorePlayersOperation(new LocalScore(sender, oldMainhand), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, mainhand))); //scoreboard players operation @s tdci_held = @s tdci_mainhand
            saveHeldItems.append(new ScorePlayersOperation(new LocalScore(sender, oldOffhand), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, offhand))); //scoreboard players operation @s tdci_held = @s tdci_mainhand
            saveHeldItems.append(new ScorePlayersOperation(new LocalScore(sender, oldHeld), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, held))); //scoreboard players operation @s tdci_held = @s tdci_mainhand
        }
        function.append(new FunctionCommand(saveHeldItems));
    }
}
