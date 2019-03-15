package com.energyxxer.trident.compiler.analyzers.type_handlers.constructors;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.textcomponents.TextComponentContext;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.default_libs.JsonLib;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class ObjectConstructors {
    private static HashMap<String, VariableMethod> constructors = new HashMap<>();

    static {
        constructors.put("int_range",
                new MethodWrapper<>("new int_range", ((instance, params) -> new IntegerRange((Integer)params[0], (Integer)params[1])), Integer.class, Integer.class).setNullable(0).setNullable(1)
                        .createForInstance(null));

        constructors.put("real_range",
                new MethodWrapper<>("new real_range", ((instance, params) -> new DoubleRange((Double)params[0], (Double)params[1])), Double.class, Double.class).setNullable(0).setNullable(1)
                        .createForInstance(null));

        constructors.put("block",ObjectConstructors::constructBlock);
        constructors.put("item",ObjectConstructors::constructItem);






        constructors.put("text_component", ObjectConstructors::constructTextComponent);
        constructors.put("nbt", ObjectConstructors::constructNBT);
    }

    private static Block constructBlock(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        CommandModule module = ctx.getCompiler().getModule();
        if(params.length == 0 || params[0] == null) return new Block(module.minecraft.types.block.get("air"));
        TridentUtil.ResourceLocation loc = assertOfType(params[0], patterns[0], ctx, TridentUtil.ResourceLocation.class);
        Namespace ns = module.getNamespace(loc.namespace);

        Type type;

        if(loc.isTag) {
            type = ns.tags.blockTags.get(loc.body);
        } else {
            type = ns.types.block.get(loc.body);
        }

        if(type == null) throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Resource location " + params[0] + " is not a valid block type", patterns[0], ctx);

        return new Block(type);
    }

    private static Item constructItem(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        CommandModule module = ctx.getCompiler().getModule();
        if(params.length == 0 || params[0] == null) return new Item(module.minecraft.types.item.get("air"));
        TridentUtil.ResourceLocation loc = assertOfType(params[0], patterns[0], ctx, TridentUtil.ResourceLocation.class);
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
        } if(params[0] instanceof ListType) {
            TagList list = new TagList();

            for(Object obj : ((ListType) params[0])) {
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
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot convert object of type '" + VariableTypeHandler.Static.getIdentifierForClass(params[0].getClass()) + "' to an nbt tag", pattern, ctx);
        } else return null;
    }

    public static VariableMethod getConstructor(String name) {
        return constructors.get(name);
    }

    private static TextComponent constructTextComponent(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if (params.length == 0) return new StringTextComponent("");
        EObject.assertNotNull(params[0], patterns[0], ctx);

        boolean skipIncompatibleTypes = false;
        if(params.length >= 2) {
            EObject.assertNotNull(params[1], patterns[1], ctx);
            skipIncompatibleTypes = assertOfType(params[1], patterns[1], ctx, Boolean.class);
        }

        try {
            JsonElement asJson = JsonLib.toJson(params[0], e -> {
                if(e instanceof TextComponent) return new TextParser.TextComponentJsonElement((TextComponent) e);
                if(e instanceof TridentUtil.ResourceLocation
                    || e instanceof Entity
                    || e instanceof CoordinateSet) return new JsonPrimitive(e.toString());
                if(e instanceof PointerType) {
                    ((PointerType) e).validate(pattern, ctx);
                    Object target = ((PointerType) e).getTarget();
                    Object member = ((PointerType) e).getMember();
                    JsonObject inner = new JsonObject();
                    if(member instanceof String) {
                        //is score
                        inner.addProperty("name", ((Entity) target).toString());
                        inner.addProperty("objective", member.toString());
                        JsonObject outer = new JsonObject();
                        outer.add("score", inner);
                        return outer;
                    } else {
                        //is nbt
                        inner.addProperty(target instanceof Entity ? "entity" : "block", target.toString());
                        inner.addProperty("nbt", member.toString());
                        return inner;
                    }
                }
                return null;
            }, skipIncompatibleTypes);
            if (asJson != null) {
                return TextParser.parseTextComponent(asJson, ctx, patterns[0], TextComponentContext.CHAT);
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot turn a value of type " + VariableTypeHandler.Static.getIdentifierForClass(params[0].getClass()) + " into a text component", patterns[0], ctx);
            }
        } catch(IllegalArgumentException x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.getMessage(), pattern, ctx);
        }
    }
}
