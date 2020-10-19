package com.energyxxer.trident.worker.tasks;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.energyxxer.util.logger.Debug;

public class SetupLibraryModuleTask extends PrismarineProjectWorkerTask {

    public static final SetupLibraryModuleTask INSTANCE = new SetupLibraryModuleTask();

    private SetupLibraryModuleTask() {}

    @Override
    public Object perform(PrismarineProjectWorker worker) throws Exception {
        CommandModule dummyModule = new CommandModule("Trident Dummy");
        dummyModule.minecraft.types.entity.create("player");
        dummyModule.minecraft.types.block.create("air");
        dummyModule.minecraft.types.item.create("air");
        dummyModule.minecraft.types.particle.create("cloud");
        dummyModule.minecraft.types.enchantment.create("protection");
        dummyModule.minecraft.types.gamerule.getOrCreate("commandBlockOutput").putProperty("argument", "boolean");
        dummyModule.minecraft.types.gamemode.create("survival");
        dummyModule.minecraft.types.dimension.create("overworld");
        dummyModule.minecraft.types.effect.create("speed");
        dummyModule.minecraft.types.slot.create("weapon.mainhand");
        dummyModule.minecraft.types.difficulty.create("normal");
        dummyModule.minecraft.types.structure.create("Village");
        dummyModule.minecraft.types.fluid.create("water");

        Debug.log("Setup library module task");

        worker.output.put(SetupModuleTask.INSTANCE, dummyModule);
        return null;
    }

    @Override
    public String getProgressMessage() {
        return "Setting up dummy command module";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[0];
    }
}
