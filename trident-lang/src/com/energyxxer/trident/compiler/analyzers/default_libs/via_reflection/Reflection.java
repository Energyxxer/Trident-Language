package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.trident.TridentFileUnitConfiguration;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;

public class Reflection {
    public static Object insertToFile(ResourceLocation targetFunction, PrimitivePrismarineFunction writer, TokenPattern<?> callingPattern, ISymbolContext ctx) {
        if(targetFunction.isTag) throw new IllegalArgumentException("Cannot insert instructions to a tag: " + targetFunction);

        TridentFile file = ctx.getCompiler().getRootCompiler().getUnit(
                TridentFileUnitConfiguration.INSTANCE,
                TridentFileUnitConfiguration.resourceLocationToFunctionPath(targetFunction)
        );
        if(file == null) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "File '" + targetFunction + "' does not exist", callingPattern, ctx);
        }

        ctx.get(SetupWritingStackTask.INSTANCE).pushWritingFile(file);
        try {
            writer.safeCall(new ActualParameterList(callingPattern), ctx, null);
        } finally {
            ctx.get(SetupWritingStackTask.INSTANCE).popWritingFile();
        }

        return null;
    }

    public static DictionaryObject getMetadata(ResourceLocation fileLoc, ISymbolContext ctx) {
        if(fileLoc.isTag) throw new IllegalArgumentException("Cannot get metadata of a tag: " + fileLoc);
        TridentFile file = ctx.getCompiler().getUnit(TridentFileUnitConfiguration.INSTANCE, TridentFileUnitConfiguration.resourceLocationToFunctionPath(fileLoc));
        if(file == null) {
            throw new IllegalArgumentException("File '" + fileLoc + "' does not exist");
        } else {
            return file.getMetadata();
        }
    }

    public static ListObject getFilesWithTag(ResourceLocation tag, ISymbolContext ctx) {
        tag = new ResourceLocation(tag.toString());
        tag.isTag = false;
        ListObject list = new ListObject(ctx.getTypeSystem());
        for(TridentFile file : ctx.getCompiler().getAllUnits(TridentFile.class)) {
            if(file.getTags().contains(tag)) {
                list.add(file.getResourceLocation());
            }
        }
        return list;
    }

    public static ListObject getFilesWithMetaTag(ResourceLocation tag, ISymbolContext ctx) {
        tag = new ResourceLocation(tag.toString());
        tag.isTag = false;
        ListObject list = new ListObject(ctx.getTypeSystem());
        for(TridentFile file : ctx.getCompiler().getRootCompiler().getAllUnits(TridentFile.class)) {
            if(file.getMetaTags().contains(tag)) {
                list.add(file.getResourceLocation());
            }
        }
        return list;
    }

    public static DictionaryObject getVisibleSymbols(ISymbolContext ctx) {
        DictionaryObject dict = new DictionaryObject(ctx.getTypeSystem());
        for(Symbol sym : ctx.collectVisibleSymbols(new HashMap<>(), ctx).values()) {
            dict.put(sym.getName(), sym.getValue(null, ctx));
        }
        return dict;
    }

    public static ResourceLocation getCurrentFile(ISymbolContext ctx) {
        return ((TridentFile) ctx.getStaticParentUnit()).getResourceLocation();
    }

    public static ResourceLocation getWritingFile(ISymbolContext ctx) {
        return ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getResourceLocation();
    }

    public static String getCurrentFilePath(ISymbolContext ctx) {
        Path path = ctx.getStaticParentUnit().getPathFromRoot();
        return path.toString().replace(File.separatorChar, '/');
    }

    public static Object getSymbol(String name, ISymbolContext ctx) {
        Symbol sym = ctx.search(name, ctx, null);
        if(sym != null) return sym.getValue(null, ctx);
        return null;
    }

    public static Object getDefinedObjectives(ISymbolContext ctx) {
        DictionaryObject objectives = new DictionaryObject(ctx.getTypeSystem());
        for(Objective objective : ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().getAll()) {
            DictionaryObject entry = new DictionaryObject(ctx.getTypeSystem());
            entry.put("name", objective.getName());
            entry.put("criterion", objective.getType());
            entry.put("displayName", objective.getDisplayName());
            objectives.put(objective.getName(), entry);
        }
        return objectives;
    }
}
