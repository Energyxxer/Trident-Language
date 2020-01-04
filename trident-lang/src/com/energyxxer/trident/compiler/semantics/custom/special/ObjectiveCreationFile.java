package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreSet;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.types.defaults.FunctionReference;

import java.util.HashSet;

public class ObjectiveCreationFile extends SpecialFile {

    private HashSet<Integer> constantsSet = null;
    private Objective constantObjective;

    public ObjectiveCreationFile(SpecialFileManager parent) {
        super(parent, "create_objectives");
    }

    @Override
    public boolean shouldForceCompile() {
        return !parent.getCompiler().getModule().getObjectiveManager().getAll().isEmpty();
    }

    public LocalScore getConstant(int con) {
        if(constantsSet == null) {
            constantsSet = new HashSet<>();
            constantObjective = parent.getCompiler().getModule().getObjectiveManager().getOrCreate("trident_const");
        }
        constantsSet.add(con);
        return new LocalScore(new PlayerName("#" + con), constantObjective);
    }

    @Override
    protected void compile() {
        parent.getCompiler().getModule().getObjectiveManager().dumpObjectiveCreators(this.function);
        if(constantsSet != null) {
            for(Integer n : constantsSet) {
                function.append(new ScoreSet(new LocalScore(new PlayerName("#"+n), constantObjective), n));
            }
        }
        Tag loadTag = parent.getCompiler().getModule().minecraft.tags.functionTags.getOrCreate("load");
        loadTag.setExport(true);
        loadTag.addValue(new FunctionReference(this.function));
    }
}
