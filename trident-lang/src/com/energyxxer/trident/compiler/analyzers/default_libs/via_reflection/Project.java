package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.versioning.ThreeNumberVersion;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

public class Project {
    public static ListObject getTargetVersion(ISymbolContext ctx) {
        ThreeNumberVersion version = (ThreeNumberVersion) ctx.get(SetupModuleTask.INSTANCE).getSettingsManager().getTargetVersion();
        return new ListObject(ctx.getTypeSystem(), new Object[] {version.getMajor(), version.getMinor(), version.getPatch()});
    }

    public static boolean getFeatureBoolean(String key, @NativeFunctionAnnotations.NullableArg Boolean defaultValue) {
        if(defaultValue == null) defaultValue = false;
        return VersionFeatureManager.getBoolean(key, defaultValue);
    }

    public static int getFeatureInt(String key, @NativeFunctionAnnotations.NullableArg java.lang.Integer defaultValue) {
        if(defaultValue == null) defaultValue = 0;
        return VersionFeatureManager.getInt(key, defaultValue);
    }

    public static String getFeatureString(String key, @NativeFunctionAnnotations.NullableArg String defaultValue) {
        return VersionFeatureManager.getString(key, defaultValue);
    }
}
