package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "Reflection")
public class ReflectionLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass reflect = new CustomClass("Reflection", "trident-util:native", globalCtx);
        reflect.seal();
        globalCtx.put(new Symbol("Reflection", Symbol.SymbolVisibility.GLOBAL, reflect));

        try {
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("getFilesWithTag", TridentUtil.ResourceLocation.class, ISymbolContext.class)));
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("getFilesWithMetaTag", TridentUtil.ResourceLocation.class, ISymbolContext.class)));
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("getMetadata", TridentUtil.ResourceLocation.class, ISymbolContext.class)));
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("getCurrentFile", ISymbolContext.class)));
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("getWritingFile", ISymbolContext.class)));
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("getSymbol", String.class, ISymbolContext.class)));
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("getVisibleSymbols", ISymbolContext.class)));
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("insertToFile", TridentUtil.ResourceLocation.class, TridentFunction.class, TokenPattern.class, ISymbolContext.class)));
            reflect.putStaticFunction(nativeMethodsToFunction(reflect.getInnerStaticContext(), ReflectionLib.class.getMethod("getDefinedObjectives", ISymbolContext.class)));
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Object insertToFile(TridentUtil.ResourceLocation targetFunction, TridentFunction writer, TokenPattern<?> callingPattern, ISymbolContext ctx) {
        if(targetFunction.isTag) throw new IllegalArgumentException("Cannot insert instructions to a tag: " + targetFunction);

        TridentFile file = ctx.getCompiler().getRootCompiler().getFile(targetFunction);
        if(file == null) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "File '" + targetFunction + "' does not exist", callingPattern, ctx);
        }

        ctx.getCompiler().pushWritingFile(file);
        try {
            writer.safeCall(new Object[0], new TokenPattern[0], callingPattern, ctx);
        } finally {
            ctx.getCompiler().popWritingFile();
        }

        return null;
    }

    public static DictionaryObject getMetadata(TridentUtil.ResourceLocation fileLoc, ISymbolContext ctx) {
        if(fileLoc.isTag) throw new IllegalArgumentException("Cannot get metadata of a tag: " + fileLoc);
        TridentFile file = ctx.getCompiler().getRootCompiler().getFile(fileLoc);
        if(file == null) {
            throw new IllegalArgumentException("File '" + fileLoc + "' does not exist");
        } else {
            return file.getMetadata();
        }
    }

    public static ListObject getFilesWithTag(TridentUtil.ResourceLocation tag, ISymbolContext ctx) {
        tag = new TridentUtil.ResourceLocation(tag.toString());
        tag.isTag = false;
        ListObject list = new ListObject();
        for(TridentFile file : ctx.getCompiler().getRootCompiler().getAllFiles()) {
            if(file.getTags().contains(tag)) {
                list.add(file.getResourceLocation());
            }
        }
        return list;
    }

    public static ListObject getFilesWithMetaTag(TridentUtil.ResourceLocation tag, ISymbolContext ctx) {
        tag = new TridentUtil.ResourceLocation(tag.toString());
        tag.isTag = false;
        ListObject list = new ListObject();
        for(TridentFile file : ctx.getCompiler().getRootCompiler().getAllFiles()) {
            if(file.getMetaTags().contains(tag)) {
                list.add(file.getResourceLocation());
            }
        }
        return list;
    }

    public static DictionaryObject getVisibleSymbols(ISymbolContext ctx) {
        DictionaryObject dict = new DictionaryObject();
        for(Symbol sym : ctx.collectVisibleSymbols(new HashMap<>(), ctx).values()) {
            dict.put(sym.getName(), sym.getValue(null, ctx));
        }
        return dict;
    }

    public static TridentUtil.ResourceLocation getCurrentFile(ISymbolContext ctx) {
        return ctx.getStaticParentFile().getResourceLocation();
    }

    public static TridentUtil.ResourceLocation getWritingFile(ISymbolContext ctx) {
        return ctx.getWritingFile().getResourceLocation();
    }

    public static Object getSymbol(String name, ISymbolContext ctx) {
        Symbol sym = ctx.search(name, ctx, null);
        if(sym != null) return sym.getValue(null, ctx);
        return null;
    }

    public static Object getDefinedObjectives(ISymbolContext ctx) {
        DictionaryObject objectives = new DictionaryObject();
        for(Objective objective : ctx.getCompiler().getRootCompiler().getModule().getObjectiveManager().getAll()) {
            DictionaryObject entry = new DictionaryObject();
            entry.put("name", objective.getName());
            entry.put("criterion", objective.getType());
            entry.put("displayName", objective.getDisplayName());
            objectives.put(objective.getName(), entry);
        }
        return objectives;
    }
}
