package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;
import java.util.Map;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "Types")
public class TypeLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        DictionaryObject block = new DictionaryObject();

        block.put("exists",
                new MethodWrapper<>("exists", ((instance, params) -> {
                    TridentUtil.ResourceLocation loc = (TridentUtil.ResourceLocation)params[0];
                    if(loc == null) return false;
                    return compiler.getRootCompiler().getModule().namespaceExists(loc.namespace) && compiler.getRootCompiler().getModule().getNamespace(loc.namespace).types.block.exists(loc.body);
                }), TridentUtil.ResourceLocation.class).createForInstance(null));
        block.put("getAll",
                new MethodWrapper<>("getAll", ((instance, params) -> {
                    ListObject all = new ListObject();
                    for(Namespace ns : compiler.getRootCompiler().getModule().getAllNamespaces()) {
                        for(Type type : ns.types.block.list()) {
                            if(!(type instanceof AliasType)) {
                                all.add(new TridentUtil.ResourceLocation(type.toString()));
                            }
                        }
                    }
                    return all;
                })).createForInstance(null));
        globalCtx.put(new Symbol("Block", Symbol.SymbolVisibility.GLOBAL, block));

        DictionaryObject item = new DictionaryObject();

        item.put("exists",
                new MethodWrapper<>("exists", ((instance, params) -> {
                    TridentUtil.ResourceLocation loc = (TridentUtil.ResourceLocation) params[0];
                    if(loc == null) return false;
                    return compiler.getRootCompiler().getModule().namespaceExists(loc.namespace) && compiler.getRootCompiler().getModule().getNamespace(loc.namespace).types.item.exists(loc.body);
                }), TridentUtil.ResourceLocation.class).createForInstance(null));
        item.put("getAll",
                new MethodWrapper<>("getAll", ((instance, params) -> {
                    ListObject all = new ListObject();
                    for(Namespace ns : compiler.getRootCompiler().getModule().getAllNamespaces()) {
                        for(Type type : ns.types.item.list()) {
                            if(!(type instanceof AliasType)) {
                                all.add(new TridentUtil.ResourceLocation(type.toString()));
                            }
                        }
                    }
                    return all;
                })).createForInstance(null));
        globalCtx.put(new Symbol("Item", Symbol.SymbolVisibility.GLOBAL, item));

        DictionaryObject type = new DictionaryObject();
        type.put("getDefinitionsForCategory", new MethodWrapper<>("getDefinitionsForCategory", ((instance, params) -> {
            Collection<Type> types = compiler.getRootCompiler().getModule().minecraft.types.getDictionary((String) params[0]).list();
            DictionaryObject obj = new DictionaryObject();
            for(Type t : types) {
                DictionaryObject inner = new DictionaryObject();
                for(Map.Entry<String, String> prop : t.getProperties().entrySet()) {
                    inner.put(prop.getKey(), prop.getValue());
                }
                obj.put(t.toString(), inner);
            }

            return obj;
        }), String.class).createForInstance(null));
        type.put("exists", (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 2) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'exists' requires 2 parameters, instead found " + params.length, pattern, file);
            }

            String category = assertOfType(params[0], patterns[0], file, String.class);
            Object rawLoc = assertOfType(params[1], patterns[1], file, TridentUtil.ResourceLocation.class, String.class);

            if(rawLoc instanceof String) {
                rawLoc = new TridentUtil.ResourceLocation((String) rawLoc);
            }

            TridentUtil.ResourceLocation loc = ((TridentUtil.ResourceLocation) rawLoc);

            if(loc.isTag) {
                return compiler.getRootCompiler().getModule().getNamespace(loc.namespace).tags.getGroup(category).exists(loc.body);
            } else {
                return compiler.getRootCompiler().getModule().getNamespace(loc.namespace).types.getDictionary(category).exists(loc.body);
            }
        });
        globalCtx.put(new Symbol("MinecraftTypes", Symbol.SymbolVisibility.GLOBAL, type));

        globalCtx.put(new Symbol("typeOf", Symbol.SymbolVisibility.GLOBAL, (VariableMethod) (params, patterns, pattern, file) ->
                (params.length >= 1 && params[0] != null) ? VariableTypeHandler.Static.getShorthandForObject(params[0]) : "null"
        ));
        globalCtx.put(new Symbol("isInstance", Symbol.SymbolVisibility.GLOBAL, new MethodWrapper<>("isInstance", (instance, params) -> {
            params[1] = ((String) params[1]).trim();
            Class cls = VariableTypeHandler.Static.getClassForShorthand((String)params[1]);
            if(cls == null) {
                throw new IllegalArgumentException("Illegal data type name '" + params[1] + "'");
            }
            if(params[0] == null) return false;
            if(params[1].equals("real") && params[0] instanceof Integer) return true;
            return cls.isInstance(params[0]);
        }, Object.class, String.class).setNullable(0).createForInstance(null)));
    }
}
