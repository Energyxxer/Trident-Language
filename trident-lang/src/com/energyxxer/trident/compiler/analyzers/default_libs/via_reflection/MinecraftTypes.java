package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

import java.util.Collection;
import java.util.Map;

public class MinecraftTypes {
    public static DictionaryObject getDefinitionsForCategory(String category, ISymbolContext ctx) {
        Collection<Type> types = ctx.get(SetupModuleTask.INSTANCE).minecraft.types.getDictionary(category).list();
        DictionaryObject obj = new DictionaryObject(ctx.getTypeSystem());
        for(Type t : types) {
            DictionaryObject inner = new DictionaryObject(ctx.getTypeSystem());
            for(Map.Entry<String, String> prop : t.getProperties().entrySet()) {
                inner.put(prop.getKey(), prop.getValue());
            }
            obj.put(t.toString(), inner);
        }

        return obj;
    }

    public static boolean exists(String category, String name, ISymbolContext ctx) {
        return exists(category, new ResourceLocation(name), ctx);
    }

    public static boolean exists(String category, ResourceLocation loc, ISymbolContext ctx) {
        if(loc.isTag) {
            return ctx.get(SetupModuleTask.INSTANCE).getNamespace(loc.namespace).tags.getGroup(category).exists(loc.body);
        } else {
            return ctx.get(SetupModuleTask.INSTANCE).getNamespace(loc.namespace).types.getDictionary(category).exists(loc.body);
        }
    }

    public static void putDefinition(String category, ResourceLocation identifier, @NativeFunctionAnnotations.NullableArg DictionaryObject properties, TokenPattern<?> pattern, ISymbolContext ctx) {
        Type type = ctx.get(SetupModuleTask.INSTANCE).getTypeManager(identifier.namespace).getDictionary(category).create(identifier.body);
        if(properties != null) {
            for(Map.Entry<String, Symbol> entry : properties.entrySet()) {
                type.putProperty(entry.getKey(), ctx.getTypeSystem().castToString(entry.getValue().getValue(pattern, ctx), pattern, ctx));
            }
        }
    }

    public static DictionaryObject getProperties(String category, ResourceLocation identifier, ISymbolContext ctx) {
        Type type = ctx.get(SetupModuleTask.INSTANCE).getTypeManager(identifier.namespace).getDictionary(category).get(identifier.body);
        if(type == null) {
            throw new IllegalArgumentException("Type '" + identifier + "' of category '" + category + "' does not exist");
        }
        DictionaryObject dict = new DictionaryObject(ctx.getTypeSystem());
        for(Map.Entry<String, String> property : type.getProperties().entrySet()) {
            dict.put(property.getKey(), property.getValue());
        }
        return dict;
    }
}
