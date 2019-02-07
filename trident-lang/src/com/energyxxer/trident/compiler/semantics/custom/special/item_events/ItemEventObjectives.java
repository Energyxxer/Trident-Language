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
        mainhand = objMgr.contains("tdci_mainhand") ? objMgr.get("tdci_mainhand") : objMgr.create("tdci_mainhand", true);
        offhand = objMgr.contains("tdci_offhand") ? objMgr.get("tdci_offhand") : objMgr.create("tdci_offhand", true);
        held = objMgr.contains("tdci_held") ? objMgr.get("tdci_held") : objMgr.create("tdci_held", true);
        oldMainhand = objMgr.contains("oldtdci_mainhand") ? objMgr.get("oldtdci_mainhand") : objMgr.create("oldtdci_mainhand", true);
        oldOffhand = objMgr.contains("oldtdci_offhand") ? objMgr.get("oldtdci_offhand") : objMgr.create("oldtdci_offhand", true);
        oldHeld = objMgr.contains("oldtdci_held") ? objMgr.get("oldtdci_held") : objMgr.create("oldtdci_held", true);
    }
}
