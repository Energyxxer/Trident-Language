package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfClass;

@AnalyzerMember(key = "com.energyxxer.trident.compiler.TridentUtil$ResourceLocation")
public class ResourceTypeHandler implements TypeHandler<TridentUtil.ResourceLocation> {
    private static HashMap<String, MemberWrapper<TridentUtil.ResourceLocation>> members = new HashMap<>();

    static {
        members.put("subLoc", new MethodWrapper<>("subLoc", (instance, params) -> {
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
            case "resolve": return (TridentMethod) (params, patterns, pattern1, ctx1) -> {
                if(params.length < 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'resolve' requires at least 1 parameter, instead found " + params.length, pattern, ctx);
                }

                Object param = assertOfClass(params[0], patterns[0], ctx1, String.class, TridentUtil.ResourceLocation.class);

                String delimiter = (params.length > 1 && params[1] != null) ? TridentMethod.HelperMethods.assertOfClass(params[1], patterns[1], ctx1, String.class) : "/";

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
        int realIndex = TridentMethod.HelperMethods.assertOfClass(index, pattern, ctx, Integer.class);

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
    public TridentMethod getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static final TridentMethod CONSTRUCTOR = (params, patterns, pattern, ctx) -> {
        if (params.length < 1) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'new resource' requires at least 2 parameters, instead found " + params.length, pattern, ctx);
        }

        TridentMethod.HelperMethods.assertOfClass(params[0], patterns[0], ctx, String.class);
        if(params.length == 1) {
            return CommonParsers.parseResourceLocation(((String) params[0]), patterns[0], ctx);
        }

        TridentMethod.HelperMethods.assertOfClass(params[1], patterns[1], ctx, ListObject.class);
        ListObject list = ((ListObject) params[1]);

        String delimiter = "/";
        if(params.length >= 3) {
            TridentMethod.HelperMethods.assertOfClass(params[2], patterns[2], ctx, String.class);
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
    };
}
