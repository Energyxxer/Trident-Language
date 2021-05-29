package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagIntArray;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;

import java.util.Random;
import java.util.UUID;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class UUIDTypeHandler implements TypeHandler<UUID> {
    private static ClassMethodFamily constructorFamily;
    private static boolean setup = false;

    private final PrismarineTypeSystem typeSystem;

    public UUIDTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    public static UUID constructUUID(ISymbolContext ctx) {
        return constructUUID(((TridentTypeSystem) ctx.getTypeSystem()).projectRandom);
    }
    public static UUID constructUUID(@NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Random") CustomClassObject randomClsObj) {
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

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        if(setup) return;
        setup = true;
        constructorFamily = new ClassMethodFamily("new");
        try {
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, null, UUIDTypeHandler.class.getMethod("constructUUID", ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, null, UUIDTypeHandler.class.getMethod("constructUUID", String.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, null, UUIDTypeHandler.class.getMethod("constructUUID", CustomClassObject.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
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
        String targetTypeIdentifier = typeSystem.getInternalTypeIdentifierForType(targetType);
        switch(targetTypeIdentifier) {
            case "primitive(nbt_value)":
            case "primitive(tag_int_array)": {
                int[] ints = new int[4];
                ints[0] = (int) (object.getMostSignificantBits() >> 32);
                ints[1] = (int) (object.getMostSignificantBits());
                ints[2] = (int) (object.getLeastSignificantBits() >> 32);
                ints[3] = (int) (object.getLeastSignificantBits());
                return new TagIntArray("", ints);
            }
        }
        throw new ClassCastException();
    }

    @Override
    public Object coerce(UUID object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if("primitive(entity)".equals(typeSystem.getInternalTypeIdentifierForType(targetType))) {
            return new PlayerName(object.toString());
        }
        return null;
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into, ISymbolContext ctx) {
        return object instanceof UUID && "primitive(entity)".equals(typeSystem.getInternalTypeIdentifierForType(into));
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
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
