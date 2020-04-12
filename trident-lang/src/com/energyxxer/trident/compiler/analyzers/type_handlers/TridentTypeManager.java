package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags.TagCompoundTypeHandler;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags.TagListTypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;

import java.util.LinkedHashMap;

public class TridentTypeManager {
    private static final LinkedHashMap<String, VariableTypeHandler<?>> PRIMITIVE_HANDLERS = new LinkedHashMap<>();

    static {

        registerTypeHandler(new BlockTypeHandler());
        registerTypeHandler(new BooleanTypeHandler());
        registerTypeHandler(new CoordinateTypeHandler());
        registerTypeHandler(new EntityTypeHandler());
        registerTypeHandler(new IntRangeTypeHandler());
        registerTypeHandler(new IntTypeHandler());
        registerTypeHandler(new ItemTypeHandler());
        registerTypeHandler(new NBTPathTypeHandler());
        registerTypeHandler(new NullTypeHandler());
        registerTypeHandler(new RealRangeTypeHandler());
        registerTypeHandler(new RealTypeHandler());
        registerTypeHandler(new ResourceTypeHandler());
        registerTypeHandler(new StringTypeHandler());
        registerTypeHandler(new TagCompoundTypeHandler());
        registerTypeHandler(new TagListTypeHandler());
        registerTypeHandler(new TextComponentTypeHandler());
        registerTypeHandler(new NBTTagTypeHandler());

        registerTypeHandler(CustomEntity.STATIC_HANDLER);
        registerTypeHandler(CustomItem.STATIC_HANDLER);
        registerTypeHandler(DictionaryObject.STATIC_HANDLER);
        registerTypeHandler(ListObject.STATIC_HANDLER);
        registerTypeHandler(TridentException.STATIC_HANDLER);
        registerTypeHandler(VariableMethod.STATIC_HANDLER);
    }

    public static void registerTypeHandler(VariableTypeHandler<?> handler) {
        if(handler.isPrimitive()) {
            PRIMITIVE_HANDLERS.put(handler.getPrimitiveShorthand(), handler);
        }
    }

    public static VariableTypeHandler<?> getHandlerForShorthand(String shorthand) {
        return PRIMITIVE_HANDLERS.get(shorthand);
    }
    public static VariableTypeHandler<?> getHandlerForObject(Object obj) {
        if(obj == null) return PRIMITIVE_HANDLERS.get("null");
        if(obj instanceof VariableTypeHandler && ((VariableTypeHandler) obj).isSelfHandler()) return (VariableTypeHandler) obj;
        VariableTypeHandler superHandler = null;
        for(VariableTypeHandler<?> handler : PRIMITIVE_HANDLERS.values()) {
            if(handler.getHandledClass() == obj.getClass()) {
                //A sure match
                return handler;
            }
            if(handler.isInstance(obj)) {
                //Found a super handler, not sure if the handler for the exact type exists though
                //This code only really works if the inheritance tree is 2-tall
                superHandler = handler;
            }
        }
        return superHandler;
    }

    public static VariableTypeHandler<?> getHandlerForHandlerClass(Class handlerClass) {
        for(VariableTypeHandler<?> handler : PRIMITIVE_HANDLERS.values()) {
            if(handler.getClass() == handlerClass) return handler;
        }
        return null;
    }
    public static VariableTypeHandler<?> getHandlerForHandledClass(Class handlingClass) {
        for(VariableTypeHandler<?> handler : PRIMITIVE_HANDLERS.values()) {
            if(handler.getHandledClass() == handlingClass) return handler;
        }
        return null;
    }

    public static String getShorthandForObject(Object param) {
        return getHandlerForObject(param).getPrimitiveShorthand();
    }
}
