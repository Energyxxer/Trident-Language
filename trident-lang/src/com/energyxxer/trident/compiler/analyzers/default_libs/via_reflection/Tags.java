package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

public class Tags {
    public static boolean tagContainsValue(String category, ResourceLocation tagLoc, ResourceLocation valueLoc, ISymbolContext ctx) {
        valueLoc = new ResourceLocation(valueLoc.toString());
        valueLoc.isTag = false;

        CommandModule module = ctx.get(SetupModuleTask.INSTANCE);

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

    private static boolean tagContainsValue(Tag tag, ResourceLocation valueLoc) {
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

    public static Object createTag(String category, ResourceLocation tagLoc, ListObject values, ISymbolContext ctx) {
        CommandModule module = ctx.get(SetupModuleTask.INSTANCE);

        Namespace ns = module.getNamespace(tagLoc.namespace);

        if(!ns.getTagManager().groupExists(category)) {
            throw new IllegalArgumentException("Invalid tag category '" + category + "'");
        }

        TagGroup tagGroup = ns.getTagManager().getGroup(category);

        Tag tag = tagGroup.getOrCreate(tagLoc.body);

        for(Object rawValue : values) {

            Object rawId;
            Tag.TagValueMode valueMode;

            if(rawValue instanceof ResourceLocation) {
                rawId = rawValue;
                valueMode = Tag.TagValueMode.REQUIRED;
            } else if(rawValue instanceof DictionaryObject) {
                rawId = ((DictionaryObject) rawValue).get("id");
                valueMode = Boolean.FALSE.equals(((DictionaryObject) rawValue).get("required")) ? Tag.TagValueMode.OPTIONAL : Tag.TagValueMode.REQUIRED;
            } else {
                throw new IllegalArgumentException("Expected resource type or dictionary in 'values' list parameter, instead got " + ctx.getTypeSystem().getTypeIdentifierForObject(rawValue));
            }

            if(rawId instanceof ResourceLocation) {
                ResourceLocation value = (ResourceLocation) rawId;
                Type type;
                if(value.isTag) {
                    type = module.getNamespace(value.namespace).getTagManager().getGroup(category).get(value.body);
                    if(type == null) {
                        throw new IllegalArgumentException("Tag '" + value + "' does not exist");
                    }
                } else {
                    type = module.getNamespace(value.namespace).getTypeManager().getDictionary(category).get(value.body);
                }
                tag.addValue(type, valueMode);
            } else {
                throw new IllegalArgumentException("Expected resource type in 'id' (in 'values' list parameter), instead got " + ctx.getTypeSystem().getTypeIdentifierForObject(rawId));
            }
        }

        return null;
    }

    public static boolean exists(String category, ResourceLocation tagLoc, ISymbolContext ctx) {
        CommandModule module = ctx.get(SetupModuleTask.INSTANCE);
        Namespace ns = module.getNamespace(tagLoc.namespace);

        if(!ns.getTagManager().groupExists(category)) {
            throw new IllegalArgumentException("Invalid tag category '" + category + "'");
        }

        TagGroup tagGroup = ns.getTagManager().getGroup(category);
        return tagGroup.exists(tagLoc.body);
    }
}
