package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;

public class Random {
    @DefaultLibraryProvider.HideFromCustomClass

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
        CustomClassObject projectRandom = cls.forceInstantiate();
        projectRandom.putHidden("random", ((TridentTypeSystem) cls.getTypeSystem()).projectRandom);

        cls.putStaticFinalMember("PROJECT_RANDOM", projectRandom);
    }
}
