package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "Tags")
public class TagLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {

        DictionaryObject tagLib = new DictionaryObject();
        tagLib.put("createTag", new MethodWrapper<>("createTag", ((instance, params) -> {
            String category = (String) params[0];
            TridentUtil.ResourceLocation tagLoc = (TridentUtil.ResourceLocation) params[1];
            ListObject values = (ListObject) params[2];

            CommandModule module = compiler.getRootCompiler().getModule();

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
                    } else {
                        type = module.getNamespace(value.namespace).getTypeManager().getDictionary(category).get(value.body);
                    }
                    tag.addValue(type);
                } else {
                    throw new IllegalArgumentException("Expected resource type in 'values' list parameter, instead got " + VariableTypeHandler.Static.getShorthandForObject(rawValue));
                }
            }

            return null;
        }), String.class, TridentUtil.ResourceLocation.class, ListObject.class).createForInstance(null));
        tagLib.put("tagContainsValue", new MethodWrapper<>("existsInTag", ((instance, params) -> {
            String category = (String) params[0];
            TridentUtil.ResourceLocation tagLoc = (TridentUtil.ResourceLocation) params[1];
            TridentUtil.ResourceLocation valueLoc = (TridentUtil.ResourceLocation) params[2];
            valueLoc = new TridentUtil.ResourceLocation(valueLoc.toString());
            valueLoc.isTag = false;

            CommandModule module = compiler.getRootCompiler().getModule();

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
        }), String.class, TridentUtil.ResourceLocation.class, TridentUtil.ResourceLocation.class).createForInstance(null));
        globalCtx.put(new Symbol("Tags", Symbol.SymbolVisibility.GLOBAL, tagLib));
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
}
