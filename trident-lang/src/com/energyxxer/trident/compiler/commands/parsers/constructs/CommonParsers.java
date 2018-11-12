package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.tags.ItemTag;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.EntityType;
import com.energyxxer.commodore.types.defaults.ItemType;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;

public class CommonParsers {
    public static EntityType parseEntityType(TokenPattern<?> id, CommandModule module) {
        TokenPattern<?> namespacePattern = id.find("NAMESPACE");
        String namespace = namespacePattern != null ? namespacePattern.flattenTokens().get(0).value : null;
        return (namespace != null ? module.getNamespace(namespace) : module.minecraft).types.entity.get(id.find("TYPE_NAME").flattenTokens().get(0).value);
    }
    public static ItemType parseItemType(TokenPattern<?> id, CommandModule module) {
        TokenPattern<?> namespacePattern = id.find("NAMESPACE");
        String namespace = namespacePattern != null ? namespacePattern.flattenTokens().get(0).value : null;
        return (namespace != null ? module.getNamespace(namespace) : module.minecraft).types.item.get(id.find("TYPE_NAME").flattenTokens().get(0).value);
    }
    public static ItemTag parseItemTag(TokenPattern<?> id, CommandModule module) {
        TokenPattern<?> namespacePattern = id.find("NAMESPACE");
        String namespace = namespacePattern != null ? namespacePattern.flattenTokens().get(0).value : null;
        return (namespace != null ? module.getNamespace(namespace) : module.minecraft).tags.itemTags.get(id.find("TYPE_NAME").flattenTokens().get(0).value);
    }

    public static Item parseItem(TokenPattern<?> pattern, CommandModule module) {
        if(pattern.getName().equals("ITEM_TAGGED") || pattern.getName().equals("ITEM")) return parseItem(((TokenStructure) pattern).getContents(), module);

        boolean isStandalone = pattern.getName().equals("CONCRETE_RESOURCE");

        Type type;

        if(isStandalone) {
            type = parseItemType(pattern.find("RESOURCE_NAME.ITEM_ID"), module);
        } else {
            type = parseItemTag(pattern.find("RESOURCE_NAME.RESOURCE_LOCATION"), module);
        }

        TagCompound tag = NBTParser.parseCompound(pattern.find(".NBT_COMPOUND"));
        return new Item(type, tag);
    }

    /**
     * CommonParsers should not be instantiated.
     * */
    private CommonParsers() {
    }
}
