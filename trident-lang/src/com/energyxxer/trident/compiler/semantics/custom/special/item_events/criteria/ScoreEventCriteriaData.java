package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventObjectives;

import java.util.ArrayList;

public class ScoreEventCriteriaData {
    public TridentCompiler compiler;
    public Type itemType;
    public Objective itemCriteriaObjective;
    public Function function;
    public CustomItem customItem;
    public ArrayList<ItemEvent> events;

    public ItemEventObjectives objectives;

    public ScoreEventCriteriaData(TridentCompiler compiler, Type itemType, Objective itemCriteriaObjective, Function function, CustomItem customItem, ArrayList<ItemEvent> events, ItemEventObjectives objectives) {
        this.compiler = compiler;
        this.itemType = itemType;
        this.itemCriteriaObjective = itemCriteriaObjective;
        this.function = function;
        this.customItem = customItem;
        this.events = events;
        this.objectives = objectives;
    }
}
