package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagByte;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.semantics.AutoPropertySymbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "com.energyxxer.commodore.item.Item")
public class ItemTypeHandler implements TypeHandler<Item> {
    private static final TridentFunction CONSTRUCTOR = (params, patterns, pattern, ctx) -> constructItem(params, patterns, pattern, ctx);

    @Override
    public Object getMember(Item object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(member.equals("itemType")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("itemType", TridentUtil.ResourceLocation.class, () -> new TridentUtil.ResourceLocation(object.getItemType().toString()), value -> {
                if(ctx.getCompiler().getModule().namespaceExists(value.namespace) && ctx.getCompiler().getModule().getNamespace(value.namespace).types.item.exists(value.body)) {
                    object.setItemType(ctx.getCompiler().getModule().getNamespace(value.namespace).types.item.get(value.body));
                } else {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, value + " is not a valid item type", pattern, ctx);
                }
            });
            return keepSymbol ? property : property.getValue(pattern, ctx);
        } else if(member.equals("itemTag")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("itemTag", TagCompound.class, object::getNBT, object::setNbt);
            return keepSymbol ? property : property.getValue(pattern, ctx);
        } else if(member.equals("getSlotNBT")) {
            return (TridentFunction) (params, patterns, pattern1, file1) -> getSlotNBT(object);
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

    @SuppressWarnings("unchecked")
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
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static Item constructItem(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        CommandModule module = ctx.getCompiler().getModule();
        if(params.length == 0 || params[0] == null) return new Item(module.minecraft.types.item.get("air"));
        TridentUtil.ResourceLocation loc = TridentFunction.HelperMethods.assertOfClass(params[0], patterns[0], ctx, TridentUtil.ResourceLocation.class);
        Namespace ns = module.getNamespace(loc.namespace);

        Type type;

        if(loc.isTag) {
            type = ns.tags.itemTags.get(loc.body);
        } else {
            type = ns.types.item.get(loc.body);
        }

        if(type == null) throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Resource location " + params[0] + " is not a valid item type", patterns[0], ctx);

        return new Item(type);
    }
}
