package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

@AnalyzerMember(key = "Types")
public class TypeLib implements DefaultLibraryProvider {
    @Override
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject block = new DictionaryObject();

        block.put("exists",
                new MethodWrapper<>("exists", ((instance, params) -> {
                    TridentUtil.ResourceLocation loc = TridentUtil.ResourceLocation.createStrict((String)params[0]);
                    if(loc == null) return false;
                    return compiler.getModule().namespaceExists(loc.namespace) && compiler.getModule().getNamespace(loc.namespace).types.block.exists(loc.body);
                }), String.class).createForInstance(null));
        stack.getGlobal().put(new Symbol("Block", Symbol.SymbolAccess.GLOBAL, block));


        stack.getGlobal().put(new Symbol("typeOf", Symbol.SymbolAccess.GLOBAL, (VariableMethod) (params, patterns, pattern, file) ->
                (params.length >= 1 && params[0] != null) ? VariableTypeHandler.Static.getShorthandForObject(params[0]) : "null"
        ));
        stack.getGlobal().put(new Symbol("isInstance", Symbol.SymbolAccess.GLOBAL, new MethodWrapper<>("isInstance", (instance, params) -> {
            params[1] = ((String) params[1]).trim();
            Class cls = VariableTypeHandler.Static.getClassForShorthand((String)params[1]);
            if(cls == null) {
                throw new IllegalArgumentException("Illegal data type name '" + params[1] + "'");
            }
            if(params[0] == null) return false;
            if(params[1].equals("real_range")) {
                return params[0] instanceof NumberRange && ((NumberRange) params[0]).getNumberClass() == Double.class;
            }
            if(params[1].equals("real") && params[0] instanceof Integer) return true;
            return cls.isInstance(params[0]);
        }, Object.class, String.class).createForInstance(null)));
    }
}
