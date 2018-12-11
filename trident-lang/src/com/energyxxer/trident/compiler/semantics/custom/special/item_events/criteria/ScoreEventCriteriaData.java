package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;

import java.util.ArrayList;

public class ScoreEventCriteriaData {
    public TridentCompiler compiler;
    public Type itemType;
    public Objective itemCriteriaObjective;
    public Function function;
    public CustomItem customItem;
    public ArrayList<FunctionReference> functionsToCall;

    public Objective mainhandObjective;
    public Objective offhandObjective;
    public Objective heldObjective;

    public Objective oldMainhandObjective;
    public Objective oldOffhandObjective;
    public Objective oldHeldObjective;

    public ScoreEventCriteriaData(TridentCompiler compiler, Type itemType, Objective itemCriteriaObjective, Function function, CustomItem customItem, ArrayList<FunctionReference> functionsToCall, Objective mainhandObjective, Objective offhandObjective, Objective heldObjective, Objective oldMainhandObjective, Objective oldOffhandObjective, Objective oldHeldObjective) {
        this.compiler = compiler;
        this.itemType = itemType;
        this.itemCriteriaObjective = itemCriteriaObjective;
        this.function = function;
        this.customItem = customItem;
        this.functionsToCall = functionsToCall;
        this.mainhandObjective = mainhandObjective;
        this.offhandObjective = offhandObjective;
        this.heldObjective = heldObjective;
        this.oldMainhandObjective = oldMainhandObjective;
        this.oldOffhandObjective = oldOffhandObjective;
        this.oldHeldObjective = oldHeldObjective;
    }
}
