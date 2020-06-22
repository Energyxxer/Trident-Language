package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction.HelperMethods.assertOfClass;
import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "com.energyxxer.trident.compiler.TridentUtil$ResourceLocation")
public class ResourceTypeHandler implements TypeHandler<TridentUtil.ResourceLocation> {
    private static HashMap<String, MemberWrapper<TridentUtil.ResourceLocation>> members = new HashMap<>();

    private static ClassMethodFamily constructorFamily;
    private static boolean setup = false;

    @Override
    public void staticTypeSetup() {
        if(setup) return;
        setup = true;
        constructorFamily = new ClassMethodFamily("new");
        try {
            constructorFamily.putOverload(new ClassMethod(CustomClass.getBaseClass(), null, nativeMethodsToFunction(null, ResourceTypeHandler.class.getMethod("constructResource", String.class, TokenPattern.class, ISymbolContext.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(CustomClass.getBaseClass(), null, nativeMethodsToFunction(null, ResourceTypeHandler.class.getMethod("constructResource", String.class, ListObject.class, String.class, TokenPattern.class, ISymbolContext.class))).setVisibility(Symbol.SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    static {
        members.put("subLoc", new NativeMethodWrapper<>("subLoc", (instance, params) -> {
            TridentUtil.ResourceLocation newLoc = new TridentUtil.ResourceLocation("a:b");
            newLoc.namespace = instance.namespace;
            newLoc.isTag = instance.isTag;

            newLoc.body = Paths.get(instance.body).subpath((int) params[0], (int) params[1]).toString();
            return newLoc;
        }, Integer.class, Integer.class));
        members.put("nameCount", new FieldWrapper<>(l -> l.getParts().length));
    }

    @Override
    public Object getMember(TridentUtil.ResourceLocation object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        switch(member) {
            case "namespace": return object.namespace;
            case "isTag": return object.isTag;
            case "body": return object.body;
            case "resolve": return (TridentFunction) (params, patterns, pattern1, ctx1) -> {
                if(params.length < 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'resolve' requires at least 1 parameter, instead found " + params.length, pattern, ctx);
                }

                Object param = assertOfClass(params[0], patterns[0], ctx1, String.class, TridentUtil.ResourceLocation.class);

                String delimiter = (params.length > 1 && params[1] != null) ? TridentFunction.HelperMethods.assertOfClass(params[1], patterns[1], ctx1, String.class) : "/";

                String other = delimiter + (param instanceof TridentUtil.ResourceLocation ? ((TridentUtil.ResourceLocation) param).body : ((String) param));

                TridentUtil.ResourceLocation newLoc = TridentUtil.ResourceLocation.createStrict(object.toString() + other);
                if(newLoc != null) {
                    return newLoc;
                } else {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "The string '" + object.toString() + other + "' cannot be used as a resource location", pattern, ctx);
                }
            };
            case "parent": {
                return object.getParent();
            }
        }
        MemberWrapper<TridentUtil.ResourceLocation> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(object);
    }

    @Override
    public Object getIndexer(TridentUtil.ResourceLocation object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        int realIndex = TridentFunction.HelperMethods.assertOfClass(index, pattern, ctx, Integer.class);

        String[] parts = object.getParts();

        if(realIndex < 0 || realIndex >= parts.length) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Index out of bounds: " + index + "; Length: " + parts.length, pattern, ctx);
        }

        return parts[realIndex];
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object cast(TridentUtil.ResourceLocation object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(TridentUtil.ResourceLocation loc) {
        return Arrays.stream(loc.getParts()).iterator();
    }

    @Override
    public Class<TridentUtil.ResourceLocation> getHandledClass() {
        return TridentUtil.ResourceLocation.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "resource";
    }

    @Override
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return constructorFamily;
    }

    public static TridentUtil.ResourceLocation constructResource(String whole, TokenPattern<?> pattern, ISymbolContext ctx) {
        return CommonParsers.parseResourceLocation(whole, pattern, ctx);
    }

    public static TridentUtil.ResourceLocation constructResource(String namespace, ListObject parts, @NativeMethodWrapper.TridentNullableArg String delimiter, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(delimiter == null) delimiter = "/";

        StringBuilder body = new StringBuilder(namespace);
        body.append(":");
        int i = 0;
        for(Object part : parts) {
            if(part instanceof String) {
                body.append(part);
            } else if(part instanceof TridentUtil.ResourceLocation) {
                body.append(((TridentUtil.ResourceLocation) part).body);
            } else {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Expected string or resource in the list, instead got: " + part + " at index " + i, pattern, ctx);
            }
            if(i < parts.size()-1) {
                body.append(delimiter);
            }
            i++;
        }

        return CommonParsers.parseResourceLocation(body.toString(), pattern, ctx);
    }

}
