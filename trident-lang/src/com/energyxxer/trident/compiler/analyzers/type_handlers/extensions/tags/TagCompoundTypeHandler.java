package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NBTToDictionary;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class TagCompoundTypeHandler implements TypeHandler<TagCompound> {
    private TypeHandlerMemberCollection<TagCompound> members;

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.RETURN_NULL);

        ClassMethodFamily constructorFamily = new ClassMethodFamily("new");
        members.setConstructor(constructorFamily);

        try {
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct"))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", int.class, Boolean.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", double.class, Boolean.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", NBTTag.class, Boolean.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", String.class, Boolean.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", boolean.class, Boolean.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", TextComponent.class, Boolean.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", ResourceLocation.class, Boolean.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", ListObject.class, Boolean.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, TagCompoundTypeHandler.class.getMethod("construct", DictionaryObject.class, Boolean.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        try {
            members.putMethod(TagCompound.class.getMethod("merge", TagCompound.class));
            members.putMethod(TagCompound.class.getMethod("remove", String.class));
            members.putMethod("toDictionary", NBTToDictionary.class.getMethod("convert", NBTTag.class, TokenPattern.class, ISymbolContext.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private final PrismarineTypeSystem typeSystem;

    public TagCompoundTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagCompound object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(object.contains(member)) return object.get(member);
        return members.getMember(object, member, pattern, ctx, keepSymbol);
    }

    @Override
    public Object getIndexer(TagCompound object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        String key = PrismarineTypeSystem.assertOfClass(index, pattern, ctx, String.class);
        if(object.contains(key)) return object.get(key);
        else return null;
    }

    @Override
    public Object cast(TagCompound object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(TagCompound object, TokenPattern<?> pattern, ISymbolContext ctx) {
        return object.getAllTags().stream().map(t -> {
            DictionaryObject entry = new DictionaryObject(typeSystem);
            entry.put("key", t.getName());
            entry.put("value", t);
            return entry;
        }).collect(Collectors.toList()).iterator();
    }

    @Override
    public Class<TagCompound> getHandledClass() {
        return TagCompound.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_compound";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }




    public static NBTTag construct() {
        return new TagCompound();
    }

    public static NBTTag construct(NBTTag tag, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes) {
        return tag.clone();
    }

    public static NBTTag construct(double value, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes) {
        return new TagDouble(value);
    }

    public static NBTTag construct(int value, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes) {
        return new TagInt(value);
    }

    public static NBTTag construct(String value, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes) {
        return new TagString(value);
    }

    public static NBTTag construct(ResourceLocation value, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes) {
        return new TagString(value.toString());
    }

    public static NBTTag construct(TextComponent value, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes) {
        return new TagString(value.toString());
    }

    public static NBTTag construct(boolean value, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes) {
        return new TagByte(value ? 1 : 0);
    }

    public static NBTTag construct(DictionaryObject dict, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes, TokenPattern<?> pattern, ISymbolContext ctx) {
        TagCompound compound = new TagCompound();

        for(Map.Entry<String, Symbol> obj : dict.entrySet()) {
            NBTTag content = constructNBT(obj.getValue().getValue(pattern, ctx), skipIncompatibleTypes, pattern, ctx);
            if(content != null) {
                content.setName(obj.getKey());
                compound.add(content);
            }
        }

        return compound;
    }

    public static NBTTag construct(ListObject listObj, @NativeFunctionAnnotations.NullableArg Boolean skipIncompatibleTypes, TokenPattern<?> pattern, ISymbolContext ctx) {
        TagList list = new TagList();

        for(Object obj : listObj) {
            NBTTag content = constructNBT(obj, skipIncompatibleTypes, pattern, ctx);
            if(content != null) {
                try {
                    list.add(content);
                } catch(CommodoreException x) {
                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Error while converting list object to nbt list: " + x.getMessage(), pattern, ctx);
                }
            }
        }

        return list;
    }

    private static NBTTag constructNBT(Object value, Boolean skipIncompatibleTypes, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(value instanceof NBTTag) return construct((NBTTag) value, skipIncompatibleTypes);
        if(value instanceof Double) return construct((double) value, skipIncompatibleTypes);
        if(value instanceof Integer) return construct((int) value, skipIncompatibleTypes);
        if(value instanceof String) return construct((String) value, skipIncompatibleTypes);
        if(value instanceof ResourceLocation) return construct((ResourceLocation) value, skipIncompatibleTypes);
        if(value instanceof TextComponent) return construct((TextComponent) value, skipIncompatibleTypes);
        if(value instanceof Boolean) return construct((boolean) value, skipIncompatibleTypes);
        if(value instanceof DictionaryObject) return construct((DictionaryObject) value, skipIncompatibleTypes, pattern, ctx);
        if(value instanceof ListObject) return construct((ListObject) value, skipIncompatibleTypes, pattern, ctx);

        if(skipIncompatibleTypes) return null;

        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot convert object of type '" + ctx.getTypeSystem().getTypeIdentifierForObject(value) + "' to an nbt tag", pattern, ctx);
    }
}
