package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.AutoPropertySymbol;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "com.energyxxer.commodore.item.Item")
public class ItemTypeHandler implements VariableTypeHandler<Item> {
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
            return keepSymbol ? property : property.getValue();
        }
        if(member.equals("itemTag")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("itemTag", TagCompound.class, object::getNBT, object::setNbt);
            return keepSymbol ? property : property.getValue();
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Item object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(Item object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }
}
