package com.energyxxer.trident.compiler.semantics.custom.special.item_events;

import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.ObjectiveManager;

public class ItemEventObjectives {
    public final Objective mainhand;
    public final Objective offhand;
    public final Objective held;
    public final Objective oldMainhand;
    public final Objective oldOffhand;
    public final Objective oldHeld;

    public ItemEventObjectives(ObjectiveManager objMgr) {
        mainhand = objMgr.getOrCreate("tdci_mainhand");
        offhand = objMgr.getOrCreate("tdci_offhand");
        held = objMgr.getOrCreate("tdci_held");
        oldMainhand = objMgr.getOrCreate("oldtdci_mainhand");
        oldOffhand = objMgr.getOrCreate("oldtdci_offhand");
        oldHeld = objMgr.getOrCreate("oldtdci_held");
    }
}
