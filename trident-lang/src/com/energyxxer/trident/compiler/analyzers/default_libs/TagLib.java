package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeMethodBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "Tags")
public class TagLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass tagLib = new CustomClass("Tags", "trident-util:native", globalCtx);
        tagLib.setConstructor(Symbol.SymbolVisibility.PRIVATE, null);
        globalCtx.put(new Symbol("Tags", Symbol.SymbolVisibility.GLOBAL, tagLib));

        try {
            tagLib.putStaticFunction(nativeMethodsToFunction(tagLib.getInnerContext(), TagLib.class.getMethod("createTag", String.class, TridentUtil.ResourceLocation.class, ListObject.class, ISymbolContext.class)));
            tagLib.putStaticFunction(nativeMethodsToFunction(tagLib.getInnerContext(), TagLib.class.getMethod("exists", String.class, TridentUtil.ResourceLocation.class, ISymbolContext.class)));
            tagLib.putStaticFunction(nativeMethodsToFunction(tagLib.getInnerContext(), TagLib.class.getMethod("tagContainsValue", String.class, TridentUtil.ResourceLocation.class, TridentUtil.ResourceLocation.class, ISymbolContext.class)));
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static boolean tagContainsValue(String category, TridentUtil.ResourceLocation tagLoc, TridentUtil.ResourceLocation valueLoc, ISymbolContext ctx) {
        valueLoc = new TridentUtil.ResourceLocation(valueLoc.toString());
        valueLoc.isTag = false;

        CommandModule module = ctx.getCompiler().getRootCompiler().getModule();

        Namespace ns = module.getNamespace(tagLoc.namespace);

        if(!ns.getTagManager().groupExists(category)) {
            throw new IllegalArgumentException("Invalid tag category '" + category + "'");
        }

        TagGroup tagGroup = ns.getTagManager().getGroup(category);
        Tag tag = tagGroup.get(tagLoc.body);

        if(tag != null) {
            return tagContainsValue(tag, valueLoc);
        } else {
            throw new IllegalArgumentException("Tag '" + tagLoc + "' does not exist");
        }
    }

    private static boolean tagContainsValue(Tag tag, TridentUtil.ResourceLocation valueLoc) {
        for(Type inTag : tag.getValues()) {
            if(inTag.toString().equals(valueLoc.toString())) {
                return true;
            } else if(inTag instanceof Tag) {
                boolean inContains = tagContainsValue(((Tag) inTag), valueLoc);
                if(inContains) return true;
            }
        }
        return false;
    }

    public static Object createTag(String category, TridentUtil.ResourceLocation tagLoc, ListObject values, ISymbolContext ctx) {
        CommandModule module = ctx.getCompiler().getRootCompiler().getModule();

        Namespace ns = module.getNamespace(tagLoc.namespace);

        if(!ns.getTagManager().groupExists(category)) {
            throw new IllegalArgumentException("Invalid tag category '" + category + "'");
        }

        TagGroup tagGroup = ns.getTagManager().getGroup(category);

        Tag tag = tagGroup.getOrCreate(tagLoc.body);

        for(Object rawValue : values) {
            if(rawValue instanceof TridentUtil.ResourceLocation) {
                TridentUtil.ResourceLocation value = (TridentUtil.ResourceLocation) rawValue;
                Type type;
                if(value.isTag) {
                    type = module.getNamespace(value.namespace).getTagManager().getGroup(category).get(value.body);
                    if(type == null) {
                        throw new IllegalArgumentException("Tag '" + value + "' does not exist");
                    }
                } else {
                    type = module.getNamespace(value.namespace).getTypeManager().getDictionary(category).get(value.body);
                }
                tag.addValue(type);
            } else {
                throw new IllegalArgumentException("Expected resource type in 'values' list parameter, instead got " + TridentTypeManager.getTypeIdentifierForObject(rawValue));
            }
        }

        return null;
    }

    public static boolean exists(String category, TridentUtil.ResourceLocation tagLoc, ISymbolContext ctx) {
        CommandModule module = ctx.getCompiler().getRootCompiler().getModule();
        Namespace ns = module.getNamespace(tagLoc.namespace);

        if(!ns.getTagManager().groupExists(category)) {
            throw new IllegalArgumentException("Invalid tag category '" + category + "'");
        }

        TagGroup tagGroup = ns.getTagManager().getGroup(category);
        return tagGroup.exists(tagLoc.body);
    }
}
