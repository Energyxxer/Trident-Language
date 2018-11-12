package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.tags.ItemTag;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.EntityType;
import com.energyxxer.commodore.types.defaults.ItemType;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.EntryParsingException;

public class CommonParsers {
    public static EntityType parseEntityType(TokenPattern<?> id, TridentCompiler compiler) {
        TokenPattern<?> namespacePattern = id.find("NAMESPACE");
        String namespace = namespacePattern != null ? namespacePattern.flattenTokens().get(0).value : null;
        return (namespace != null ? compiler.getModule().getNamespace(namespace) : compiler.getModule().minecraft).types.entity.get(id.find("TYPE_NAME").flattenTokens().get(0).value);
    }
    public static ItemType parseItemType(TokenPattern<?> id, TridentCompiler compiler) {
        TokenPattern<?> namespacePattern = id.find("NAMESPACE");
        String namespace = namespacePattern != null ? namespacePattern.flattenTokens().get(0).value : null;
        return (namespace != null ? compiler.getModule().getNamespace(namespace) : compiler.getModule().minecraft).types.item.get(id.find("TYPE_NAME").flattenTokens().get(0).value);
    }
    public static ItemTag parseItemTag(TokenPattern<?> id, TridentCompiler compiler) {
        String str = id.flattenTokens().get(0).value;
        String namespace = "minecraft";
        if(str.contains(":")) {
            namespace = str.substring(0, str.indexOf(':'));
            str = str.substring(str.indexOf(":")+1);
        }
        ItemTag returned = compiler.getModule().getNamespace(namespace).tags.itemTags.get(str);
        if(returned == null) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "No such item tag exists: #" + namespace + ":" + str, id));
            throw new EntryParsingException();
        }
        return returned;
    }

    public static Item parseItem(TokenPattern<?> pattern, TridentCompiler compiler) {
        if(pattern.getName().equals("ITEM_TAGGED") || pattern.getName().equals("ITEM")) return parseItem(((TokenStructure) pattern).getContents(), compiler);

        boolean isStandalone = pattern.getName().equals("CONCRETE_RESOURCE");

        Type type;

        if(isStandalone) {
            type = parseItemType(pattern.find("RESOURCE_NAME.ITEM_ID"), compiler);
        } else {
            type = parseItemTag(pattern.find("RESOURCE_NAME.RESOURCE_LOCATION"), compiler);
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
