package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

public class Item {
    public static boolean exists(ResourceLocation loc, ISymbolContext ctx) {
        return ctx.get(SetupModuleTask.INSTANCE).namespaceExists(loc.namespace) && ctx.get(SetupModuleTask.INSTANCE).getNamespace(loc.namespace).types.item.exists(loc.body);
    }

    public static ListObject getAll(ISymbolContext ctx) {
        ListObject all = new ListObject(ctx.getTypeSystem());
        for(Namespace ns : ctx.get(SetupModuleTask.INSTANCE).getAllNamespaces()) {
            for(Type type : ns.types.item.list()) {
                if(!(type instanceof AliasType)) {
                    all.add(new ResourceLocation(type.toString()));
                }
            }
        }
        return all;
    }
}
