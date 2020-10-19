package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.trident.worker.tasks.SetupPropertiesTask;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.util.JsonTraverser;

public class Random {
    @DefaultLibraryProvider.HideFromCustomClass
    public static java.util.Random PROJECT_RANDOM;

    public static void __new(@NativeFunctionAnnotations.ThisArg CustomClassObject thiz) {
        thiz.putHidden("random", new java.util.Random());
    }

    public static void __new(int seed, @NativeFunctionAnnotations.ThisArg CustomClassObject thiz) {
        thiz.putHidden("random", new java.util.Random(seed));
    }

    public static int nextInt(@NativeFunctionAnnotations.ThisArg CustomClassObject thiz) {
        return ((java.util.Random) thiz.getHidden("random")).nextInt();
    }

    public static int nextInt(int bound, @NativeFunctionAnnotations.ThisArg CustomClassObject thiz) {
        return ((java.util.Random) thiz.getHidden("random")).nextInt(bound);
    }

    public static double nextReal(@NativeFunctionAnnotations.ThisArg CustomClassObject thiz) {
        return ((java.util.Random) thiz.getHidden("random")).nextDouble();
    }

    public static double nextGaussian(@NativeFunctionAnnotations.ThisArg CustomClassObject thiz) {
        return ((java.util.Random) thiz.getHidden("random")).nextGaussian();
    }

    public static boolean nextBoolean(@NativeFunctionAnnotations.ThisArg CustomClassObject thiz) {
        return ((java.util.Random) thiz.getHidden("random")).nextBoolean();
    }

    public static void __onClassCreation(CustomClass cls) {
        PrismarineCompiler compiler = cls.getInnerStaticContext().getCompiler();
        int defaultSeed = compiler.getRootDir().getName().hashCode();

        int projectSeed = JsonTraverser.INSTANCE.reset(compiler.get(SetupPropertiesTask.INSTANCE)).get("random-seed").asInt(defaultSeed);

        CustomClassObject projectRandom = cls.forceInstantiate();
        projectRandom.putHidden("random", PROJECT_RANDOM = new java.util.Random(projectSeed));

        cls.putStaticFinalMember("PROJECT_RANDOM", projectRandom);
    }
}
