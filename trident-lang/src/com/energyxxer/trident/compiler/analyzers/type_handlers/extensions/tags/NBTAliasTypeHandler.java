package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;

import java.util.Map;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfType;

public class NBTAliasTypeHandler implements TypeHandler<TagCompound> {
    private static final TridentMethod CONSTRUCTOR = NBTAliasTypeHandler::constructNBT;

    @Override
    public Object getMember(TagCompound object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagCompound object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(TagCompound object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<TagCompound> getHandledClass() {
        return TagCompound.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "nbt";
    }

    @Override
    public TridentMethod getConstructor() {
        return CONSTRUCTOR;
    }

    private static NBTTag constructNBT(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0) return new TagCompound();
        EObject.assertNotNull(params[0], patterns[0], ctx);

        boolean skipIncompatibleTypes = false;
        if(params.length >= 2) {
            EObject.assertNotNull(params[1], patterns[1], ctx);
            skipIncompatibleTypes = assertOfType(params[1], patterns[1], ctx, Boolean.class);
        }

        if(params[0] instanceof NBTTag) return ((NBTTag) params[0]).clone();
        if(params[0] instanceof Number) {
            if(params[0] instanceof Double) {
                return new TagDouble(((double) params[0]));
            } else {
                return new TagInt((int) params[0]);
            }
        } else if(params[0] instanceof String || params[0] instanceof TridentUtil.ResourceLocation || params[0] instanceof TextComponent) {
            return new TagString(params[0].toString());
        } else if(params[0] instanceof Boolean) {
            return new TagByte((boolean)params[0] ? 1 : 0);
        } else if(params[0] instanceof DictionaryObject) {
            TagCompound compound = new TagCompound();

            for(Map.Entry<String, Symbol> obj : ((DictionaryObject) params[0]).entrySet()) {
                NBTTag content = constructNBT(new Object[] {obj.getValue().getValue(), skipIncompatibleTypes}, new TokenPattern[] {patterns[0], pattern}, pattern, ctx);
                if(content != null) {
                    content.setName(obj.getKey());
                    compound.add(content);
                }
            }

            return compound;
        } if(params[0] instanceof ListObject) {
            TagList list = new TagList();

            for(Object obj : ((ListObject) params[0])) {
                NBTTag content = constructNBT(new Object[] {obj, skipIncompatibleTypes}, new TokenPattern[] {patterns[0], pattern}, pattern, ctx);
                if(content != null) {
                    try {
                        list.add(content);
                    } catch(CommodoreException x) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Error while converting list object to nbt list: " + x.getMessage(), pattern, ctx);
                    }
                }
            }

            return list;
        } else if(!skipIncompatibleTypes) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot convert object of type '" + TridentTypeManager.getTypeIdentifierForObject(params[0]) + "' to an nbt tag", pattern, ctx);
        } else return null;
    }
}
