package com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation;

import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScorePlayersOperation;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFile;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventObjectives;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;

public class SaveHeldItemsFile extends SpecialFile {

    private ItemEventObjectives objectives;

    public SaveHeldItemsFile(SpecialFileManager parent) {
        super(parent, "save_held_items");
    }

    @Override
    public boolean shouldForceCompile() {
        return false;
    }

    @Override
    protected void compile() {
        Selector sender = new Selector(SENDER);

        function.append(new ScorePlayersOperation(new LocalScore(sender, objectives.oldMainhand), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, objectives.mainhand))); //scoreboard players operation @s tdci_held = @s tdci_mainhand
        function.append(new ScorePlayersOperation(new LocalScore(sender, objectives.oldOffhand), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, objectives.offhand))); //scoreboard players operation @s tdci_held = @s tdci_mainhand
        function.append(new ScorePlayersOperation(new LocalScore(sender, objectives.oldHeld), ScorePlayersOperation.Operation.ASSIGN, new LocalScore(sender, objectives.held))); //scoreboard players operation @s tdci_held = @s tdci_mainhand
    }

    public void setObjectives(ItemEventObjectives objectives) {
        this.objectives = objectives;
    }
}
