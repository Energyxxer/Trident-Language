package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.versioning.ThreeNumberVersion;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NativeMethodWrapper;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "Project")
public class ProjectLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass project = new CustomClass("Project", "trident-util:native", globalCtx);
        project.setNoConstructor();
        globalCtx.put(new Symbol("Project", Symbol.SymbolVisibility.GLOBAL, project));

        try {
            project.putStaticFunction(nativeMethodsToFunction(project.getInnerStaticContext(), ProjectLib.class.getMethod("getTargetVersion", ISymbolContext.class)));
            project.putStaticFunction(nativeMethodsToFunction(project.getInnerStaticContext(), ProjectLib.class.getMethod("getFeatureBoolean", String.class, Boolean.class)));
            project.putStaticFunction(nativeMethodsToFunction(project.getInnerStaticContext(), ProjectLib.class.getMethod("getFeatureInt", String.class, Integer.class)));
            project.putStaticFunction(nativeMethodsToFunction(project.getInnerStaticContext(), ProjectLib.class.getMethod("getFeatureString", String.class, String.class)));
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static ListObject getTargetVersion(ISymbolContext ctx) {
        ThreeNumberVersion version = (ThreeNumberVersion) ctx.getCompiler().getModule().getSettingsManager().getTargetVersion();
        return new ListObject(new Object[] {version.getMajor(), version.getMinor(), version.getPatch()});
    }

    public static boolean getFeatureBoolean(String key, @NativeMethodWrapper.TridentNullableArg Boolean defaultValue) {
        if(defaultValue == null) defaultValue = false;
        return VersionFeatureManager.getBoolean(key, defaultValue);
    }

    public static int getFeatureInt(String key, @NativeMethodWrapper.TridentNullableArg Integer defaultValue) {
        if(defaultValue == null) defaultValue = 0;
        return VersionFeatureManager.getInt(key, defaultValue);
    }

    public static String getFeatureString(String key, @NativeMethodWrapper.TridentNullableArg String defaultValue) {
        return VersionFeatureManager.getString(key, defaultValue);
    }
}
