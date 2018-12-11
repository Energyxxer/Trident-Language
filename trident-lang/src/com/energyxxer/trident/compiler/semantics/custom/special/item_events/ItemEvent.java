package com.energyxxer.trident.compiler.semantics.custom.special.item_events;

import com.energyxxer.commodore.types.defaults.FunctionReference;

public class ItemEvent {

    public enum ItemScoreEventType {
        USED, BROKEN, DROPPED, PICKED_UP
    }

    public FunctionReference toCall;
    public boolean pure = false;

    public ItemEvent(FunctionReference toCall) {
        this(toCall, false);
    }

    public ItemEvent(FunctionReference toCall, boolean pure) {
        this.toCall = toCall;
        this.pure = pure;
    }
}
