package com.energyxxer.trident.compiler.analyzers.type_handlers.constructors;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.*;
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
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
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


        constructors.put("resource", (params, patterns, pattern, ctx) -> {
            if (params.length < 1) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'new resource' requires at least 2 parameters, instead found " + params.length, pattern, ctx);
            }

            assertOfType(params[0], patterns[0], ctx, String.class);
            if(params.length == 1) {
                return CommonParsers.parseResourceLocation(((String) params[0]), patterns[0], ctx);
            }

            assertOfType(params[1], patterns[1], ctx, ListObject.class);
            ListObject list = ((ListObject) params[1]);

            String delimiter = "/";
            if(params.length >= 3) {
                assertOfType(params[2], patterns[2], ctx, String.class);
                delimiter = (String) params[2];
            }

            StringBuilder body = new StringBuilder((String)params[0]);
            body.append(":");
            int i = 0;
            for(Object part : list) {
                if(part instanceof String) {
                    body.append(part);
                } else if(part instanceof TridentUtil.ResourceLocation) {
                    body.append(((TridentUtil.ResourceLocation) part).body);
                } else {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Expected string or resource in the list, instead got: " + part + " at index " + i, patterns[1], ctx);
                }
                if(i < ((ListObject) params[1]).size()-1) {
                    body.append(delimiter);
                }
                i++;
            }

            return CommonParsers.parseResourceLocation(body.toString(), pattern, ctx);
        });


        /*constructors.put("resource", new MethodWrapper<>("new resource", ((instance, params) -> {
            if(params[1] == null) {
                TridentUtil.ResourceLocation result = TridentUtil.ResourceLocation.createStrict(((String) params[0]));
                if(result != null) return result;
                else throw new IllegalArgumentException("The string '" + params[0] + "' cannot be used as a resource location");
            }
            StringBuilder body = new StringBuilder((String)params[0]);
            body.append(":");
            String delimiter = params[2] != null ? ((String) params[2]) : "/";
            int i = 0;
            for(Object part : ((ListObject) params[1])) {
                if(part instanceof String) {
                    body.append(part);
                } else if(part instanceof TridentUtil.ResourceLocation) {
                    body.append(((TridentUtil.ResourceLocation) part).body);
                } else {
                    throw new IllegalArgumentException("Expected string or resource_location in the list, instead got: " + part + " at index " + i);
                }
                if(i < ((ListObject) params[1]).size()-1) {
                    body.append(delimiter);
                }
                i++;
            }

            TridentUtil.ResourceLocation result = TridentUtil.ResourceLocation.createStrict(body.toString());
            if(result != null) return result;
            else throw new IllegalArgumentException("The string '" + body.toString() + "' cannot be used as a resource location");
        }), String.class, ListObject.class, String.class).setNullable(1).setNullable(2).createForInstance(null));*/


        constructors.put("text_component", ObjectConstructors::constructTextComponent);
        constructors.put("nbt", ObjectConstructors::constructNBT);
        constructors.put("nbt_path", ObjectConstructors::constructNBTPath);

        constructors.put("tag_byte",
                new MethodWrapper<>("new tag_byte", ((instance, params) -> new TagByte(params[0] == null ? 0 : (int)params[0])), Integer.class).setNullable(0)
                        .createForInstance(null));
        constructors.put("tag_short",
                new MethodWrapper<>("new tag_short", ((instance, params) -> new TagShort(params[0] == null ? 0 : (int)params[0])), Integer.class).setNullable(0)
                        .createForInstance(null));
        constructors.put("tag_int",
                new MethodWrapper<>("new tag_int", ((instance, params) -> new TagInt(params[0] == null ? 0 : (int)params[0])), Integer.class).setNullable(0)
                        .createForInstance(null));
        constructors.put("tag_float",
                new MethodWrapper<>("new tag_float", ((instance, params) -> new TagFloat(params[0] == null ? 0 : (float)(double)params[0])), Double.class).setNullable(0)
                        .createForInstance(null));
        constructors.put("tag_double",
                new MethodWrapper<>("new tag_double", ((instance, params) -> new TagDouble(params[0] == null ? 0 : (double)params[0])), Double.class).setNullable(0)
                        .createForInstance(null));
        constructors.put("tag_long", ObjectConstructors::constructTagLong);
        constructors.put("tag_string",
                new MethodWrapper<>("new tag_string", ((instance, params) -> new TagString(params[0] == null ? "" : (String)params[0])), String.class).setNullable(0)
                        .createForInstance(null));
        constructors.put("tag_byte_array", ObjectConstructors::constructTagByteArray);
        constructors.put("tag_int_array", ObjectConstructors::constructTagIntArray);
        constructors.put("tag_long_array", ObjectConstructors::constructTagLongArray);
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
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot convert object of type '" + VariableTypeHandler.Static.getIdentifierForClass(params[0].getClass()) + "' to an nbt tag", pattern, ctx);
        } else return null;
    }

    private static NBTPath constructNBTPath(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new NBTPath(new NBTListMatch());

        Object obj = assertOfType(params[0], patterns[0], ctx, String.class, Integer.class, TagCompound.class);
        if(obj instanceof TagCompound) {
            boolean wrapInList = params.length >= 2 && params[1] instanceof Boolean && ((Boolean) params[1]);
            if(wrapInList) {
                return new NBTPath(new NBTListMatch((TagCompound) obj));
            } else {
                return new NBTPath(new NBTPathCompoundRoot((TagCompound) obj));
            }
        }
        if(obj instanceof String) {
            TagCompound compoundMatch = null;
            if(params.length >= 2 && params[1] != null) {
                compoundMatch = assertOfType(params[1], patterns[1], ctx, TagCompound.class);
            }
            return new NBTPath(new NBTPathKey((String) obj, compoundMatch));
        }
        if(obj instanceof Integer) return new NBTPath(new NBTPathIndex((int) obj));
        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
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
                if(e instanceof PointerObject) {
                    ((PointerObject) e).validate(pattern, ctx);
                    Object target = ((PointerObject) e).getTarget();
                    Object member = ((PointerObject) e).getMember();
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

    @SuppressWarnings("unchecked")
    private static TagLong constructTagLong(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagLong(0L);
        Object param = assertOfType(params[0], patterns[0], ctx, Integer.class, Double.class, String.class);
        if(param instanceof String) {
            try {
                return new TagLong(Long.parseLong((String) param));
            } catch(NumberFormatException x) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, x.getMessage(), pattern, ctx);
            }
        } else if(param instanceof Double) {
            return new TagLong((long)(double) param);
        } else {
            return new TagLong((long)(int) param);
        }
    }

    private static TagByteArray constructTagByteArray(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagByteArray();
        ListObject list = assertOfType(params[0], patterns[0], ctx, ListObject.class);

        TagByteArray arr = new TagByteArray();

        for(Object obj : list) {
            arr.add(new TagByte(assertOfType(obj, patterns[0], ctx, Integer.class)));
        }

        return arr;
    }

    private static TagIntArray constructTagIntArray(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagIntArray();
        ListObject list = assertOfType(params[0], patterns[0], ctx, ListObject.class);

        TagIntArray arr = new TagIntArray();

        for(Object obj : list) {
            arr.add(new TagInt(assertOfType(obj, patterns[0], ctx, Integer.class)));
        }

        return arr;
    }

    private static TagIntArray constructTagLongArray(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagIntArray();
        ListObject list = assertOfType(params[0], patterns[0], ctx, ListObject.class);

        TagIntArray arr = new TagIntArray();

        for(Object obj : list) {
            arr.add(constructTagLong(new Object[] {obj}, patterns, pattern, ctx));
        }

        return arr;
    }

    public static VariableMethod getConstructor(String name) {
        return constructors.get(name);
    }
}
