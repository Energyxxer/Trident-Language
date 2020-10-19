package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.commodore.types.defaults.BlockType;
import com.energyxxer.commodore.types.defaults.EntityType;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.commodore.types.defaults.ItemType;

public class TridentSuggestionTags {
    public static final String IDENTIFIER = "suggestion.trident.identifier";
    public static final String IDENTIFIER_NEW = "suggestion.trident.identifier.new";
    public static final String IDENTIFIER_EXISTING = "csk:suggestion.trident.identifier.existing";
    public static final String IDENTIFIER_MEMBER = "suggestion.trident.identifier.member";

    public static final String OBJECTIVE_EXISTING = "csk:suggestion.trident.objective.existing";

    public static final String BOOLEAN = "csk:suggestion.trident.boolean";

    public static final String __TYPE_TEMPLATE = "csk:suggestion.trident.type.";
    public static final String BLOCK = __TYPE_TEMPLATE + BlockType.CATEGORY;
    public static final String ITEM = __TYPE_TEMPLATE + ItemType.CATEGORY;
    public static final String ENTITY_TYPE = __TYPE_TEMPLATE + EntityType.CATEGORY;
    public static final String TYPE = "csk:suggestion.trident.type";
    public static final String PRIMITIVE_TYPE = "csk:primitive_type";

    public static final String RESOURCE = "csk:suggestion.trident.resource";
    public static final String TRIDENT_FUNCTION = "csk:suggestion.trident.trident_function";
    public static final String FUNCTION = "csk:suggestion.trident.function";
    public static final String SOUND_RESOURCE = "csk:suggestion.trident.sound_resource";

    public static final String __TAG_TEMPLATE = "csk:suggestion.trident.tag.";
    public static final String BLOCK_TAG = "csk:suggestion.trident.tag." + BlockType.CATEGORY;
    public static final String ITEM_TAG = "csk:suggestion.trident.tag." + ItemType.CATEGORY;
    public static final String ENTITY_TYPE_TAG = "csk:suggestion.trident.tag." + EntityType.CATEGORY;
    public static final String FUNCTION_TAG = "csk:suggestion.trident.tag." + FunctionReference.CATEGORY;

    public static final String CONTEXT_ENTRY = "ctx:trident.entry";
    public static final String CONTEXT_COMMAND = "ctx:trident.command";
    public static final String CONTEXT_MODIFIER = "ctx:trident.modifier";
    public static final String CONTEXT_ENTITY_BODY = "ctx:trident.entity_body";
    public static final String CONTEXT_ITEM_BODY = "ctx:trident.item_body";
    public static final String CONTEXT_CLASS_BODY = "ctx:trident.class_body";
    public static final String CONTEXT_INTERPOLATION_VALUE = "ctx:trident.interpolation_value";



    public static final String TAG_COMMAND = "cst:command";
    public static final String TAG_MODIFIER = "cst:modifier";
    public static final String TAG_INSTRUCTION = "cst:instruction";

    public static final String TAG_VARIABLE = "cst:variable";
    public static final String TAG_ITEM = "cst:item";
    public static final String TAG_ENTITY = "cst:entity";
    public static final String TAG_COORDINATE = "cst:coordinate";
    public static final String TAG_CUSTOM_ENTITY = "cst:custom_entity";
    public static final String TAG_ENTITY_COMPONENT = "cst:entity_component";
    public static final String TAG_ENTITY_EVENT = "cst:entity_event";
    public static final String TAG_CUSTOM_ITEM = "cst:custom_item";
    public static final String TAG_CLASS = "cst:class";
    public static final String TAG_OBJECTIVE = "cst:objective";
    public static final String TAG_FIELD = "cst:class_field";
    public static final String TAG_METHOD = "cst:class_method";
}
