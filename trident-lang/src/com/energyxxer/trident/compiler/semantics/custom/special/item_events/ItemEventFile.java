package com.energyxxer.trident.compiler.semantics.custom.special.item_events;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreReset;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.ObjectiveManager;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFile;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria.ScoreEventCriteriaData;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria.ScoreEventCriteriaHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.ALL_PLAYERS;
import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;

public class ItemEventFile extends SpecialFile {
    private final HashMap<ItemEvent.ItemScoreEventType, HashMap<Type, HashMap<CustomItem, ArrayList<ItemEvent>>>> definedItems = new HashMap<>();

    public ItemEventObjectives objectives;

    public ItemEventFile(SpecialFileManager parent) {
        super(parent, "item_events");
    }

    public void addCustomItem(ItemEvent.ItemScoreEventType eventType, Type itemType, CustomItem customItem, ItemEvent event) {
        if(!definedItems.containsKey(eventType)) definedItems.put(eventType, new HashMap<>());
        if(!definedItems.get(eventType).containsKey(itemType)) {
            definedItems.get(eventType).put(itemType, new HashMap<>());
            definedItems.get(eventType).get(itemType).put(null, new ArrayList<>());
        }
        if(!definedItems.get(eventType).get(itemType).containsKey(customItem)) definedItems.get(eventType).get(itemType).put(customItem, new ArrayList<>());
        definedItems.get(eventType).get(itemType).get(customItem).add(event);
    }

    @Override
    public boolean shouldForceCompile() {
        return !definedItems.isEmpty();
    }

    public void compile() {
        for(ItemEvent.ItemScoreEventType eventType : definedItems.keySet()) {
            AnalyzerManager.getAnalyzer(ScoreEventCriteriaHandler.class, eventType.name().toLowerCase()).globalStart(parent);
        }

        parent.getTickFunction().append(new ExecuteCommand(new FunctionCommand(function), new ExecuteAsEntity(new Selector(ALL_PLAYERS)), new ExecuteAtEntity(new Selector(SENDER))));

        ObjectiveManager objMgr = compiler.getModule().getObjectiveManager();
        objectives = new ItemEventObjectives(objMgr);

        for(ItemEvent.ItemScoreEventType eventType : definedItems.keySet()) {
            AnalyzerManager.getAnalyzer(ScoreEventCriteriaHandler.class, eventType.name().toLowerCase()).start(parent, this);
        }

        for(Map.Entry<ItemEvent.ItemScoreEventType, HashMap<Type, HashMap<CustomItem, ArrayList<ItemEvent>>>> eventEntry : definedItems.entrySet()) {
            ItemEvent.ItemScoreEventType eventType = eventEntry.getKey();
            for(Map.Entry<Type, HashMap<CustomItem, ArrayList<ItemEvent>>> typeEntry : eventEntry.getValue().entrySet()) {
                Type itemType = typeEntry.getKey();

                String criteria = "minecraft." + eventType.name().toLowerCase() + ":" + itemType.toString().replace(':','.');
                Objective objective = objMgr.create(eventType.name().toLowerCase().charAt(0) + "item." + Integer.toString(new TridentUtil.ResourceLocation(itemType.toString()).toString().hashCode(), 16), criteria, new StringTextComponent(eventType.name().toLowerCase() + " item " + itemType), true);

                ScoreArgument scores = new ScoreArgument();
                scores.put(objective, new NumberRange<>(1, null));

                for(Map.Entry<CustomItem, ArrayList<ItemEvent>> itemEntry : typeEntry.getValue().entrySet()) {
                    ScoreEventCriteriaData data = new ScoreEventCriteriaData(compiler, itemType, objective, function, itemEntry.getKey(), itemEntry.getValue(), objectives);
                    AnalyzerManager.getAnalyzer(ScoreEventCriteriaHandler.class, eventType.name().toLowerCase()).mid(data);
                }

                function.append(new ScoreReset(new Selector(SENDER, scores), objective));
            }
        }
        
        for(ItemEvent.ItemScoreEventType eventType : definedItems.keySet()) {
            AnalyzerManager.getAnalyzer(ScoreEventCriteriaHandler.class, eventType.name().toLowerCase()).end(parent, this);
        }

        for(ItemEvent.ItemScoreEventType eventType : definedItems.keySet()) {
            AnalyzerManager.getAnalyzer(ScoreEventCriteriaHandler.class, eventType.name().toLowerCase()).globalEnd(parent);
        }
    }
}
