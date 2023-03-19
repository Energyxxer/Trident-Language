package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.NativeMethodWrapper;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class ResourceTypeHandler implements TypeHandler<ResourceLocation> {
    private TypeHandlerMemberCollection<ResourceLocation> members;

    private final PrismarineTypeSystem typeSystem;

    public ResourceTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        ClassMethodFamily constructorFamily = new ClassMethodFamily("new");
        members.setConstructor(constructorFamily);
        try {
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, ResourceTypeHandler.class.getMethod("constructResource", String.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, globalCtx, ResourceTypeHandler.class.getMethod("constructResource", String.class, ListObject.class, String.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        members.putReadOnlyField("namespace", l -> l.namespace);
        members.putReadOnlyField("body", l -> l.body);
        members.putReadOnlyField("isTag", l -> l.isTag);
        members.putReadOnlyField("parent", ResourceLocation::getParent);
        members.putReadOnlyField("nameCount", l -> l.getParts().length);

        ClassMethodFamily resolveFamily = new ClassMethodFamily("resolve");
        resolveFamily.setUseExternalThis(true);
        members.put("resolve", new NativeMethodWrapper<>(resolveFamily));
        try {
            resolveFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, ResourceTypeHandler.class.getMethod("resolve", String.class, String.class, ResourceLocation.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            resolveFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, ResourceTypeHandler.class.getMethod("resolve", ResourceLocation.class, String.class, ResourceLocation.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);

            members.putMethod("subLoc", ResourceTypeHandler.class.getMethod("subLoc", int.class, int.class, ResourceLocation.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(ResourceLocation object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx, keepSymbol);
    }

    @Override
    public Object getIndexer(ResourceLocation object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        int realIndex = PrismarineTypeSystem.assertOfClass(index, pattern, ctx, Integer.class);

        String[] parts = object.getParts();

        if(realIndex < 0 || realIndex >= parts.length) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Index out of bounds: " + index + "; Length: " + parts.length, pattern, ctx);
        }

        return parts[realIndex];
    }

    @Override
    public Object cast(ResourceLocation object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Iterator<?> getIterator(ResourceLocation loc, TokenPattern<?> pattern, ISymbolContext ctx) {
        return Arrays.stream(loc.getParts()).iterator();
    }

    @Override
    public Class<ResourceLocation> getHandledClass() {
        return ResourceLocation.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "resource";
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }




    public static ResourceLocation constructResource(String whole, TokenPattern<?> pattern, ISymbolContext ctx) {
        return CommonParsers.parseResourceLocation(whole, pattern, ctx);
    }

    public static ResourceLocation constructResource(String namespace, ListObject parts, @NativeFunctionAnnotations.NullableArg String delimiter, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(delimiter == null) delimiter = "/";

        StringBuilder body = new StringBuilder(namespace);
        body.append(":");
        int i = 0;
        for(Object part : parts) {
            if(part instanceof String) {
                body.append(part);
            } else if(part instanceof ResourceLocation) {
                body.append(((ResourceLocation) part).body);
            } else {
                throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Expected string or resource in the list, instead got: " + part + " at index " + i, pattern, ctx);
            }
            if(i < parts.size()-1) {
                body.append(delimiter);
            }
            i++;
        }

        return CommonParsers.parseResourceLocation(body.toString(), pattern, ctx);
    }

    public static ResourceLocation resolve(ResourceLocation nextPart, @NativeFunctionAnnotations.NullableArg String delimiter, @NativeFunctionAnnotations.ThisArg ResourceLocation parent, TokenPattern<?> pattern, ISymbolContext ctx) {
        return resolve(nextPart.body, delimiter, parent, pattern, ctx);
    }

    public static ResourceLocation resolve(String nextPart, @NativeFunctionAnnotations.NullableArg String delimiter, @NativeFunctionAnnotations.ThisArg ResourceLocation parent, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(delimiter == null) delimiter = "/";

        String other = delimiter + nextPart;

        ResourceLocation newLoc = ResourceLocation.createStrict(parent.toString() + other);
        if(newLoc != null) {
            return newLoc;
        } else {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "The string '" + parent.toString() + other + "' cannot be used as a resource location", pattern, ctx);
        }
    }

    public static ResourceLocation subLoc(int start, int end, @NativeFunctionAnnotations.ThisArg ResourceLocation instance) {
        ResourceLocation newLoc = new ResourceLocation("a:b");
        newLoc.namespace = instance.namespace;
        newLoc.isTag = instance.isTag;

        newLoc.body = Paths.get(instance.body).subpath(start, end).toString();
        return newLoc;
    }
}
