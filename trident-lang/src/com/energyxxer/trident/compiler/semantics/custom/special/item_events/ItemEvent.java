package com.energyxxer.trident.compiler.semantics.custom.special.item_events;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.types.defaults.FunctionReference;

import java.util.ArrayList;

public class ItemEvent {

    public enum ItemScoreEventType {
        USED, BROKEN, DROPPED, PICKED_UP
    }

    public FunctionReference toCall;
    public boolean pure = false;
    public ArrayList<ExecuteModifier> modifiers;

    public ItemEvent(FunctionReference toCall, boolean pure, ArrayList<ExecuteModifier> modifiers) {
        this.toCall = toCall;
        this.pure = pure;
        this.modifiers = modifiers;
    }
}
