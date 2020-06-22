package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NativeMethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.HashMap;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.nbt.path.NBTPath")
public class NBTPathTypeHandler implements TypeHandler<NBTPath> {
    private static HashMap<String, MemberWrapper<NBTPath>> members = new HashMap<>();

    private static ClassMethodFamily constructorFamily;
    private static boolean setup = false;

    @Override
    public void staticTypeSetup() {
        if(setup) return;
        setup = true;
        constructorFamily = new ClassMethodFamily("new");
        try {
            constructorFamily.putOverload(new ClassMethod(CustomClass.getBaseClass(), null, nativeMethodsToFunction(null, NBTPathTypeHandler.class.getMethod("constructNBTPath"))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(CustomClass.getBaseClass(), null, nativeMethodsToFunction(null, NBTPathTypeHandler.class.getMethod("constructNBTPath", int.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(CustomClass.getBaseClass(), null, nativeMethodsToFunction(null, NBTPathTypeHandler.class.getMethod("constructNBTPath", String.class, TagCompound.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(CustomClass.getBaseClass(), null, nativeMethodsToFunction(null, NBTPathTypeHandler.class.getMethod("constructNBTPath", TagCompound.class, Boolean.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    static {
        members.put("resolveKey", new NativeMethodWrapper<NBTPath>("resolveKey", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTPathKey((String) params[0], (TagCompound) params[1]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, String.class, TagCompound.class).setNullable(1));
        members.put("resolveIndex", new NativeMethodWrapper<>("resolveIndex", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTPathIndex((int) params[0]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, Integer.class));
        members.put("resolveListMatch", new NativeMethodWrapper<NBTPath>("resolveListMatch", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTListMatch((TagCompound) params[0]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, TagCompound.class).setNullable(0));
    }

    @Override
    public Object getMember(NBTPath path, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        MemberWrapper<NBTPath> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(path);
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
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return constructorFamily;
    }

    public static NBTPath constructNBTPath() {
        return new NBTPath(new NBTListMatch());
    }

    public static NBTPath constructNBTPath(String key, @NativeMethodWrapper.TridentNullableArg TagCompound compoundMatch) {
        return new NBTPath(new NBTPathKey(key, compoundMatch));
    }

    public static NBTPath constructNBTPath(TagCompound compound, @NativeMethodWrapper.TridentNullableArg Boolean wrapInList) {
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
}
