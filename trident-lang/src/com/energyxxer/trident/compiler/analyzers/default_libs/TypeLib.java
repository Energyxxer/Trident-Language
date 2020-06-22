package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;
import java.util.Map;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "Types")
public class TypeLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass block = new CustomClass("Block", "trident-util:native", globalCtx);
        block.seal();
        globalCtx.put(new Symbol("Block", Symbol.SymbolVisibility.GLOBAL, block));

        try {
            block.putStaticFunction(nativeMethodsToFunction(block.getInnerStaticContext(), "exists", TypeLib.class.getMethod("blockExists", TridentUtil.ResourceLocation.class, ISymbolContext.class)));
            block.putStaticFunction(nativeMethodsToFunction(block.getInnerStaticContext(), "getAll", TypeLib.class.getMethod("getAllBlocks", ISymbolContext.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        CustomClass item = new CustomClass("Item", "trident-util:native", globalCtx);
        item.seal();
        globalCtx.put(new Symbol("Item", Symbol.SymbolVisibility.GLOBAL, item));


        try {
            item.putStaticFunction(nativeMethodsToFunction(item.getInnerStaticContext(), "exists", TypeLib.class.getMethod("itemExists", TridentUtil.ResourceLocation.class, ISymbolContext.class)));
            item.putStaticFunction(nativeMethodsToFunction(item.getInnerStaticContext(), "getAll", TypeLib.class.getMethod("getAllItems", ISymbolContext.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        CustomClass minecraftTypes = new CustomClass("MinecraftTypes", "trident-util:native", globalCtx);
        minecraftTypes.seal();
        globalCtx.put(new Symbol("MinecraftTypes", Symbol.SymbolVisibility.GLOBAL, minecraftTypes));
        try {
            minecraftTypes.putStaticFunction(nativeMethodsToFunction(minecraftTypes.getInnerStaticContext(), TypeLib.class.getMethod("getDefinitionsForCategory", String.class, ISymbolContext.class)));
            minecraftTypes.putStaticFunction(nativeMethodsToFunction(minecraftTypes.getInnerStaticContext(), TypeLib.class.getMethod("exists", String.class, TridentUtil.ResourceLocation.class, ISymbolContext.class)));
            minecraftTypes.putStaticFunction(nativeMethodsToFunction(minecraftTypes.getInnerStaticContext(), TypeLib.class.getMethod("exists", String.class, String.class, ISymbolContext.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        try {
            globalCtx.put(new Symbol("isInstance", Symbol.SymbolVisibility.GLOBAL, nativeMethodsToFunction(globalCtx, TypeLib.class.getMethod("isInstance", Object.class, String.class))));
            globalCtx.put(new Symbol("typeOf", Symbol.SymbolVisibility.GLOBAL, nativeMethodsToFunction(globalCtx, TridentTypeManager.class.getMethod("getTypeIdentifierForObject", Object.class))));
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
        globalCtx.put(new Symbol("type_definition", Symbol.SymbolVisibility.GLOBAL, TridentTypeManager.getTypeHandlerTypeHandler()));
    }

    public static boolean isInstance(@NativeMethodWrapper.TridentNullableArg Object obj, String typeName) {
        typeName = typeName.trim();
        TypeHandler handler = TridentTypeManager.getPrimitiveHandlerForShorthand(typeName);
        if(obj == null) return "null".equals(typeName);
        if(handler == null) {
            throw new IllegalArgumentException("Illegal primitive data type name '" + typeName + "'");
        }
        return handler.isInstance(obj);
    }

    public static boolean blockExists(TridentUtil.ResourceLocation loc, ISymbolContext ctx) {
        return ctx.getCompiler().getRootCompiler().getModule().namespaceExists(loc.namespace) && ctx.getCompiler().getRootCompiler().getModule().getNamespace(loc.namespace).types.block.exists(loc.body);
    }

    public static ListObject getAllBlocks(ISymbolContext ctx) {
        ListObject all = new ListObject();
        for(Namespace ns : ctx.getCompiler().getRootCompiler().getModule().getAllNamespaces()) {
            for(Type type : ns.types.block.list()) {
                if(!(type instanceof AliasType)) {
                    all.add(new TridentUtil.ResourceLocation(type.toString()));
                }
            }
        }
        return all;
    }

    public static boolean itemExists(TridentUtil.ResourceLocation loc, ISymbolContext ctx) {
        return ctx.getCompiler().getRootCompiler().getModule().namespaceExists(loc.namespace) && ctx.getCompiler().getRootCompiler().getModule().getNamespace(loc.namespace).types.item.exists(loc.body);
    }

    public static ListObject getAllItems(ISymbolContext ctx) {
        ListObject all = new ListObject();
        for(Namespace ns : ctx.getCompiler().getRootCompiler().getModule().getAllNamespaces()) {
            for(Type type : ns.types.item.list()) {
                if(!(type instanceof AliasType)) {
                    all.add(new TridentUtil.ResourceLocation(type.toString()));
                }
            }
        }
        return all;
    }

    public static DictionaryObject getDefinitionsForCategory(String category, ISymbolContext ctx) {
        Collection<Type> types = ctx.getCompiler().getRootCompiler().getModule().minecraft.types.getDictionary(category).list();
        DictionaryObject obj = new DictionaryObject();
        for(Type t : types) {
            DictionaryObject inner = new DictionaryObject();
            for(Map.Entry<String, String> prop : t.getProperties().entrySet()) {
                inner.put(prop.getKey(), prop.getValue());
            }
            obj.put(t.toString(), inner);
        }

        return obj;
    }

    public static boolean exists(String category, String name, ISymbolContext ctx) {
        return exists(category, new TridentUtil.ResourceLocation(name), ctx);
    }

    public static boolean exists(String category, TridentUtil.ResourceLocation loc, ISymbolContext ctx) {
        if(loc.isTag) {
            return ctx.getCompiler().getRootCompiler().getModule().getNamespace(loc.namespace).tags.getGroup(category).exists(loc.body);
        } else {
            return ctx.getCompiler().getRootCompiler().getModule().getNamespace(loc.namespace).types.getDictionary(category).exists(loc.body);
        }
    }
}
