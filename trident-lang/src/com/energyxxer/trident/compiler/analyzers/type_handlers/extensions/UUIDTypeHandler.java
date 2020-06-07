package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.default_libs.RandomLib;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NativeMethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Random;
import java.util.UUID;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "java.util.UUID")
public class UUIDTypeHandler implements TypeHandler<UUID> {
    private static ClassMethodFamily constructorFamily;

    public static UUID constructUUID() {
        return constructUUID(RandomLib.PROJECT_RANDOM);
    }
    public static UUID constructUUID(@NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Random") CustomClassObject randomClsObj) {
        return constructUUID((Random) randomClsObj.getHidden("random"));
    }
    private static UUID constructUUID(Random random) {
        long mostSigBits = random.nextLong();
        long leastSigBits = random.nextLong();

        mostSigBits &= 0xffffffffffff0fffL; /* clear version        */
        mostSigBits |= 0x0000000000004000L; /* set to version 4     */

        leastSigBits &= 0x3fffffffffffffffL; /* clear variant        */
        leastSigBits |= 0x8000000000000000L; /* set to IETF variant  */

        return new UUID(mostSigBits, leastSigBits);
    }
    public static UUID constructUUID(String str) {
        return UUID.fromString(str);
    }

    static {
        constructorFamily = new ClassMethodFamily("new");
        try {
            constructorFamily.putOverload(new ClassMethod(CustomClass.BASE_CLASS, null, nativeMethodsToFunction(null, UUIDTypeHandler.class.getMethod("constructUUID"))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(CustomClass.BASE_CLASS, null, nativeMethodsToFunction(null, UUIDTypeHandler.class.getMethod("constructUUID", String.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(CustomClass.BASE_CLASS, null, nativeMethodsToFunction(null, UUIDTypeHandler.class.getMethod("constructUUID", CustomClassObject.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(UUID object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(UUID object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(UUID object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return constructorFamily;
    }

    @Override
    public Class<UUID> getHandledClass() {
        return UUID.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "uuid";
    }
}
