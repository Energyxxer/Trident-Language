package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NativeMethodWrapper;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.*;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.util.JsonTraverser;

import java.util.Random;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "Random")
public class RandomLib implements DefaultLibraryProvider {
    public static Random PROJECT_RANDOM;

    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass random = new CustomClass("Random", "trident-util:native", globalCtx);
        globalCtx.put(new Symbol("Random", Symbol.SymbolVisibility.GLOBAL, random));

        ClassMethodFamily constructors = random.createConstructorFamily();
        ClassMethodTable instanceMethods = random.getInstanceMethods();

        try {
            constructors.putOverload(new ClassMethod(random, null, nativeMethodsToFunction(random.getInnerStaticContext(), RandomLib.class.getMethod("construct", CustomClassObject.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructors.putOverload(new ClassMethod(random, null, nativeMethodsToFunction(random.getInnerStaticContext(), RandomLib.class.getMethod("construct", int.class, CustomClassObject.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            instanceMethods.put(new ClassMethod(random, null, nativeMethodsToFunction(random.getInnerStaticContext(), RandomLib.class.getMethod("nextInt", CustomClassObject.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            instanceMethods.put(new ClassMethod(random, null, nativeMethodsToFunction(random.getInnerStaticContext(), RandomLib.class.getMethod("nextInt", int.class, CustomClassObject.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            instanceMethods.put(new ClassMethod(random, null, nativeMethodsToFunction(random.getInnerStaticContext(), RandomLib.class.getMethod("nextReal", CustomClassObject.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            instanceMethods.put(new ClassMethod(random, null, nativeMethodsToFunction(random.getInnerStaticContext(), RandomLib.class.getMethod("nextGaussian", CustomClassObject.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            instanceMethods.put(new ClassMethod(random, null, nativeMethodsToFunction(random.getInnerStaticContext(), RandomLib.class.getMethod("nextBoolean", CustomClassObject.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }

        int defaultSeed = compiler.getRootDir().getName().hashCode();

        int projectSeed = JsonTraverser.INSTANCE.reset(compiler.getProperties()).get("random-seed").asInt(defaultSeed);

        CustomClassObject projectRandom = random.forceInstantiate();
        projectRandom.putHidden("random", PROJECT_RANDOM = new Random(projectSeed));

        random.putStaticFinalMember("PROJECT_RANDOM", projectRandom);

        CustomClass.updateStringIdentifiedClassListener(random);
    }

    public static void construct(@NativeMethodWrapper.TridentThisArg CustomClassObject thiz) {
        thiz.putHidden("random", new Random());
    }

    public static void construct(int seed, @NativeMethodWrapper.TridentThisArg CustomClassObject thiz) {
        thiz.putHidden("random", new Random(seed));
    }

    public static int nextInt(@NativeMethodWrapper.TridentThisArg CustomClassObject thiz) {
        return ((Random) thiz.getHidden("random")).nextInt();
    }

    public static int nextInt(int bound, @NativeMethodWrapper.TridentThisArg CustomClassObject thiz) {
        return ((Random) thiz.getHidden("random")).nextInt(bound);
    }

    public static double nextReal(@NativeMethodWrapper.TridentThisArg CustomClassObject thiz) {
        return ((Random) thiz.getHidden("random")).nextDouble();
    }

    public static double nextGaussian(@NativeMethodWrapper.TridentThisArg CustomClassObject thiz) {
        return ((Random) thiz.getHidden("random")).nextGaussian();
    }

    public static boolean nextBoolean(@NativeMethodWrapper.TridentThisArg CustomClassObject thiz) {
        return ((Random) thiz.getHidden("random")).nextBoolean();
    }
}
