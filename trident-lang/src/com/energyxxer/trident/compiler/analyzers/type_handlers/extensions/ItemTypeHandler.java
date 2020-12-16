package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagByte;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.AutoPropertySymbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;

public class ItemTypeHandler implements TypeHandler<Item> {
    private static final PrimitivePrismarineFunction CONSTRUCTOR = (params, ctx, thisObject) -> constructItem(params, ctx);

    private final PrismarineTypeSystem typeSystem;

    public ItemTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(Item object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        switch (member) {
            case "itemType": {
                AutoPropertySymbol property = new AutoPropertySymbol<>("itemType", ResourceLocation.class, () -> new ResourceLocation(object.getItemType().toString()), value -> {
                    if (ctx.get(SetupModuleTask.INSTANCE).namespaceExists(value.namespace) && ctx.get(SetupModuleTask.INSTANCE).getNamespace(value.namespace).types.item.exists(value.body)) {
                        object.setItemType(ctx.get(SetupModuleTask.INSTANCE).getNamespace(value.namespace).types.item.get(value.body));
                    } else {
                        throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, value + " is not a valid item type", pattern, ctx);
                    }
                });
                return keepSymbol ? property : property.getValue(pattern, ctx);
            }
            case "itemTag": {
                AutoPropertySymbol property = new AutoPropertySymbol<>("itemTag", TagCompound.class, object::getNBT, object::setNbt);
                return keepSymbol ? property : property.getValue(pattern, ctx);
            }
            case "getSlotNBT":
                return (PrimitivePrismarineFunction) (params, ctx1, thisObject) -> getSlotNBT(object);
        }
        throw new MemberNotFoundException();
    }

    public static TagCompound getSlotNBT(Item item) {
        TagCompound nbt = new TagCompound(
                new TagString("id", item.getItemType().toString()),
                new TagByte("Count", 1));
        if(item.getNBT() != null) {
            TagCompound tag = item.getNBT().clone();
            tag.setName("tag");
            nbt = new TagCompound(tag).merge(nbt);
        }
        return nbt;
    }

    @Override
    public Object getIndexer(Item object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Item object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<Item> getHandledClass() {
        return Item.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "item";
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return CONSTRUCTOR;
    }

    private static Item constructItem(ActualParameterList params, ISymbolContext ctx) {
        CommandModule module = ctx.get(SetupModuleTask.INSTANCE);
        if(params.size() == 0 || params.getValue(0) == null) return new Item(module.minecraft.types.item.get("air"));
        ResourceLocation loc = PrismarineTypeSystem.assertOfClass(params.getValue(0), params.getPattern(0), ctx, ResourceLocation.class);
        Namespace ns = module.getNamespace(loc.namespace);

        Type type;

        if(loc.isTag) {
            type = ns.tags.itemTags.get(loc.body);
        } else {
            type = ns.types.item.get(loc.body);
        }

        if(type == null) throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Resource location " + params.getValue(0) + " is not a valid item type", params.getPattern(0), ctx);

        return new Item(type);
    }
}
