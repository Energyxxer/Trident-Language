package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "Reflection")
public class ReflectionLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        DictionaryObject reflect = new DictionaryObject();

        reflect.put("getFilesWithTag",
                new MethodWrapper<>("getFilesWithTag", ((instance, params) -> getFilesWithTag(((TridentUtil.ResourceLocation) params[0]), compiler.getRootCompiler())), TridentUtil.ResourceLocation.class).createForInstance(null));
        reflect.put("getFilesWithMetaTag",
                new MethodWrapper<>("getFilesWithMetaTag", ((instance, params) -> getFilesWithMetaTag(((TridentUtil.ResourceLocation) params[0]), compiler.getRootCompiler())), TridentUtil.ResourceLocation.class).createForInstance(null));
        reflect.put("getMetadata",
                new MethodWrapper<>("getMetadata", ((instance, params) -> getMetadata(((TridentUtil.ResourceLocation) params[0]), compiler.getRootCompiler())), TridentUtil.ResourceLocation.class).createForInstance(null));
        reflect.put("getCurrentFile", (VariableMethod) (params, patterns, pattern, ctx) -> ctx.getStaticParentFile().getResourceLocation());
        reflect.put("getWritingFile", (VariableMethod) (params, patterns, pattern, ctx) -> ctx.getWritingFile().getResourceLocation());
        reflect.put("getSymbol", (VariableMethod) (params, patterns, pattern, ctx) -> {
            if(params.length < 1) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'getSymbol' requires 1 parameters, instead found " + params.length, pattern, ctx);
            }
            String symbolName = assertOfType(params[0], patterns[0], ctx, String.class);
            Symbol sym = ctx.search(symbolName, ctx);
            if(sym != null) return sym.getValue();
            return null;
        });
        reflect.put("getVisibleSymbols", (VariableMethod) (params, patterns, pattern, ctx) -> getVisibleSymbols(ctx));
        reflect.put("insertToFile", (VariableMethod) this::insertToFile);
        globalCtx.put(new Symbol("Reflection", Symbol.SymbolVisibility.GLOBAL, reflect));
    }

    private Object insertToFile(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length < 2) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'insertToFile' requires 2 parameters, instead found " + params.length, pattern, ctx);
        }
        TridentUtil.ResourceLocation fileLoc = assertOfType(params[0], patterns[0], ctx, TridentUtil.ResourceLocation.class);
        VariableMethod func = assertOfType(params[1], patterns[1], ctx, VariableMethod.class);
        if(fileLoc.isTag) throw new IllegalArgumentException("Cannot insert instructions to a tag: " + fileLoc);

        TridentFile file = ctx.getCompiler().getFile(fileLoc);
        if(file == null) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "File '" + fileLoc + "' does not exist", pattern, ctx);
        }

        ctx.getCompiler().pushWritingFile(file);
        try {
            func.safeCall(new Object[0], new TokenPattern[0], pattern, ctx);
        } finally {
            ctx.getCompiler().popWritingFile();
        }

        return null;
    }

    private DictionaryObject getMetadata(TridentUtil.ResourceLocation fileLoc, TridentCompiler compiler) {
        if(fileLoc.isTag) throw new IllegalArgumentException("Cannot get metadata of a tag: " + fileLoc);
        TridentFile file = compiler.getFile(fileLoc);
        if(file == null) {
            throw new IllegalArgumentException("File '" + fileLoc + "' does not exist");
        } else {
            return file.getMetadata();
        }
    }

    private ListObject getFilesWithTag(TridentUtil.ResourceLocation tag, TridentCompiler compiler) {
        tag = new TridentUtil.ResourceLocation(tag.toString());
        tag.isTag = false;
        ListObject list = new ListObject();
        for(TridentFile file : compiler.getAllFiles()) {
            if(file.getTags().contains(tag)) {
                list.add(file.getResourceLocation());
            }
        }
        return list;
    }

    private ListObject getFilesWithMetaTag(TridentUtil.ResourceLocation tag, TridentCompiler compiler) {
        tag = new TridentUtil.ResourceLocation(tag.toString());
        tag.isTag = false;
        ListObject list = new ListObject();
        for(TridentFile file : compiler.getAllFiles()) {
            if(file.getMetaTags().contains(tag)) {
                list.add(file.getResourceLocation());
            }
        }
        return list;
    }

    private DictionaryObject getVisibleSymbols(ISymbolContext ctx) {
        DictionaryObject dict = new DictionaryObject();
        for(Symbol sym : ctx.collectVisibleSymbols(new HashMap<>(), ctx).values()) {
            dict.put(sym.getName(), sym.getValue());
        }
        return dict;
    }
}
