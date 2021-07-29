package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;

import java.util.ArrayList;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class NBTPathTypeHandler implements TypeHandler<NBTPath> {
    private TypeHandlerMemberCollection<NBTPath> members;

    private final PrismarineTypeSystem typeSystem;

    public NBTPathTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        ClassMethodFamily constructorFamily = new ClassMethodFamily("new");
        members.setConstructor(constructorFamily);
        try {
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, NBTPathTypeHandler.class.getMethod("constructNBTPath"))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, NBTPathTypeHandler.class.getMethod("constructNBTPath", int.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, NBTPathTypeHandler.class.getMethod("constructNBTPath", String.class, TagCompound.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, NBTPathTypeHandler.class.getMethod("constructNBTPath", TagCompound.class, Boolean.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        try {
            members.putMethod(NBTPathTypeHandler.class.getMethod("resolveKey", String.class, TagCompound.class, NBTPath.class));
            members.putMethod(NBTPathTypeHandler.class.getMethod("resolveIndex", int.class, NBTPath.class));
            members.putMethod(NBTPathTypeHandler.class.getMethod("resolveListMatch", TagCompound.class, NBTPath.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(NBTPath path, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(path, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(NBTPath object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(NBTPath object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<NBTPath> getHandledClass() {
        return NBTPath.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "nbt_path";
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }

    public static NBTPath constructNBTPath() {
        return new NBTPath(new NBTListMatch());
    }

    public static NBTPath constructNBTPath(String key, @NativeFunctionAnnotations.NullableArg TagCompound compoundMatch) {
        return new NBTPath(new NBTPathKey(key, compoundMatch));
    }

    public static NBTPath constructNBTPath(TagCompound compound, @NativeFunctionAnnotations.NullableArg Boolean wrapInList) {
        if(wrapInList == null) wrapInList = false;
        if(wrapInList) {
            return new NBTPath(new NBTListMatch(compound));
        } else {
            return new NBTPath(new NBTPathCompoundRoot(compound));
        }
    }

    public static NBTPath constructNBTPath(int listIndex) {
        return new NBTPath(new NBTPathIndex(listIndex));
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static NBTPath resolveKey(String key, @NativeFunctionAnnotations.NullableArg TagCompound compound, @NativeFunctionAnnotations.ThisArg NBTPath instance) {
        ArrayList<NBTPathNode> nodes = new ArrayList<>();
        for (NBTPath subPath : instance) {
            nodes.add(subPath.getNode());
        }
        nodes.add(new NBTPathKey(key, compound));
        return new NBTPath(nodes.toArray(new NBTPathNode[0]));
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static NBTPath resolveIndex(int index, @NativeFunctionAnnotations.ThisArg NBTPath instance) {
        ArrayList<NBTPathNode> nodes = new ArrayList<>();
        for (NBTPath subPath : instance) {
            nodes.add(subPath.getNode());
        }
        nodes.add(new NBTPathIndex(index));
        return new NBTPath(nodes.toArray(new NBTPathNode[0]));
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static NBTPath resolveListMatch(@NativeFunctionAnnotations.NullableArg TagCompound compound, @NativeFunctionAnnotations.ThisArg NBTPath instance) {
        ArrayList<NBTPathNode> nodes = new ArrayList<>();
        for (NBTPath subPath : instance) {
            nodes.add(subPath.getNode());
        }
        nodes.add(new NBTListMatch(compound));
        return new NBTPath(nodes.toArray(new NBTPathNode[0]));
    }
}
