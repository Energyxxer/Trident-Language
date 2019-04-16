package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "Types")
public class TypeLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        DictionaryObject block = new DictionaryObject();

        block.put("exists",
                new MethodWrapper<>("exists", ((instance, params) -> {
                    TridentUtil.ResourceLocation loc = (TridentUtil.ResourceLocation)params[0];
                    if(loc == null) return false;
                    return compiler.getModule().namespaceExists(loc.namespace) && compiler.getModule().getNamespace(loc.namespace).types.block.exists(loc.body);
                }), TridentUtil.ResourceLocation.class).createForInstance(null));
        block.put("getAll",
                new MethodWrapper<>("getAll", ((instance, params) -> {
                    ListObject all = new ListObject();
                    for(Namespace ns : compiler.getModule().getAllNamespaces()) {
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
                    return compiler.getModule().namespaceExists(loc.namespace) && compiler.getModule().getNamespace(loc.namespace).types.item.exists(loc.body);
                }), TridentUtil.ResourceLocation.class).createForInstance(null));
        item.put("getAll",
                new MethodWrapper<>("getAll", ((instance, params) -> {
                    ListObject all = new ListObject();
                    for(Namespace ns : compiler.getModule().getAllNamespaces()) {
                        for(Type type : ns.types.item.list()) {
                            if(!(type instanceof AliasType)) {
                                all.add(new TridentUtil.ResourceLocation(type.toString()));
                            }
                        }
                    }
                    return all;
                })).createForInstance(null));
        globalCtx.put(new Symbol("Item", Symbol.SymbolVisibility.GLOBAL, item));


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
