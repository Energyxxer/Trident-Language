package com.energyxxer.trident.worker.tasks;

import com.energyxxer.enxlex.lexical_analysis.token.BuiltinSource;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.nbtmapper.packs.NBTTypeMapPack;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.energyxxer.trident.Trident;
import com.energyxxer.trident.compiler.TridentBuildConfiguration;

public class SetupTypeMapTask extends PrismarineProjectWorkerTask<NBTTypeMap> {

    public static final SetupTypeMapTask INSTANCE = new SetupTypeMapTask();

    private SetupTypeMapTask() {}

    @Override
    public NBTTypeMap perform(PrismarineProjectWorker worker) throws Exception {
        NBTTypeMap typeMap = new NBTTypeMap(worker.output.get(SetupModuleTask.INSTANCE));

        TridentBuildConfiguration buildConfig = worker.output.get(SetupBuildConfigTask.INSTANCE);

        if(buildConfig.typeMapPacks != null) {
            for(NBTTypeMapPack pack : buildConfig.typeMapPacks) {
                typeMap.parsing.parsePack(pack);
            }
        } else {
            typeMap.parsing.parseNBTTMFile(new BuiltinSource("common.nbttm"), Trident.Resources.defaults.get("common.nbttm"));
            typeMap.parsing.parseNBTTMFile(new BuiltinSource("entities.nbttm"), Trident.Resources.defaults.get("entities.nbttm"));
            typeMap.parsing.parseNBTTMFile(new BuiltinSource("block_entities.nbttm"), Trident.Resources.defaults.get("block_entities.nbttm"));
        }
        typeMap.parsing.parseNBTTMFile(new BuiltinSource("trident.nbttm"), Trident.Resources.defaults.get("trident.nbttm"));

        if(worker.report != null) {
            worker.report.addNotices(typeMap.getNotices());
        }

        return typeMap;
    }

    @Override
    public String getProgressMessage() {
        return "Setting up NBT Type Map";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupBuildConfigTask.INSTANCE, SetupModuleTask.INSTANCE};
    }
}
