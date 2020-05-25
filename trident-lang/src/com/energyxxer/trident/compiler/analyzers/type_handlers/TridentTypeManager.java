package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags.*;
import com.energyxxer.trident.compiler.semantics.ILazyValue;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.entities.EntityEvent;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;

import java.util.LinkedHashMap;

public class TridentTypeManager {
    private static final LinkedHashMap<String, TypeHandler<?>> PRIMITIVE_HANDLERS = new LinkedHashMap<>();
    private static final TypeHandlerTypeHandler TYPE_HANDLER_TYPE_HANDLER;

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
        registerTypeHandler(new NBTAliasTypeHandler());
        registerTypeHandler(new TagByteTypeHandler());
        registerTypeHandler(new TagShortTypeHandler());
        registerTypeHandler(new TagIntTypeHandler());
        registerTypeHandler(new TagFloatTypeHandler());
        registerTypeHandler(new TagDoubleTypeHandler());
        registerTypeHandler(new TagLongTypeHandler());
        registerTypeHandler(new TagStringTypeHandler());

        registerTypeHandler(new TagByteArrayTypeHandler());
        registerTypeHandler(new TagIntArrayTypeHandler());
        registerTypeHandler(new TagLongArrayTypeHandler());

        registerTypeHandler(new NBTTagTypeHandler());

        registerTypeHandler(CustomEntity.STATIC_HANDLER);
        registerTypeHandler(CustomItem.STATIC_HANDLER);
        registerTypeHandler(EntityEvent.STATIC_HANDLER);
        registerTypeHandler(DictionaryObject.STATIC_HANDLER);
        registerTypeHandler(ListObject.STATIC_HANDLER);
        registerTypeHandler(TridentException.STATIC_HANDLER);
        registerTypeHandler(TridentFunction.STATIC_HANDLER);

        registerTypeHandler(TYPE_HANDLER_TYPE_HANDLER = new TypeHandlerTypeHandler());
    }

    public static void registerTypeHandler(TypeHandler<?> handler) {
        if(handler.isPrimitive()) {
            PRIMITIVE_HANDLERS.put(handler.getTypeIdentifier(), handler);
        }
    }

    public static TypeHandler<?> getPrimitiveHandlerForShorthand(String shorthand) {
        return PRIMITIVE_HANDLERS.get(shorthand);
    }
    public static TypeHandler<?> getHandlerForObject(Object obj) {
        if(obj == null) return PRIMITIVE_HANDLERS.get("null");
        if(obj instanceof TypeHandler && ((TypeHandler) obj).isSelfHandler()) return (TypeHandler) obj;
        TypeHandler superHandler = null;
        for(TypeHandler<?> handler : PRIMITIVE_HANDLERS.values()) {
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

    public static TypeHandler<?> getHandlerForHandlerClass(Class handlerClass) {
        for(TypeHandler<?> handler : PRIMITIVE_HANDLERS.values()) {
            if(handler.getClass() == handlerClass) return handler;
        }
        return null;
    }
    public static TypeHandler<?> getHandlerForHandledClass(Class handlingClass) {
        for(TypeHandler<?> handler : PRIMITIVE_HANDLERS.values()) {
            if(handler.getHandledClass() == handlingClass) return handler;
        }
        return null;
    }

    public static String getTypeIdentifierForObject(Object param) {
        return (!(param instanceof TypeHandler<?>) || !((TypeHandler) param).isStaticHandler()) ? getHandlerForObject(param).getTypeIdentifier() : "type_definition<" + ((TypeHandler) param).getTypeIdentifier() + ">";
    }

    public static String getTypeIdentifierForType(TypeHandler<?> handler) {
        return handler.getTypeIdentifier();
    }

    public static boolean isStaticPrimitiveHandler(TypeHandler<?> handler) {
        return PRIMITIVE_HANDLERS.values().contains(handler);
    }


    public static String getInternalTypeIdentifierForObject(Object obj) {
        if(obj instanceof ILazyValue) {
            return "lazy";
        }
        if(obj instanceof Symbol) {
            return "symbol";
        }
        TypeHandler handler = TridentTypeManager.getHandlerForObject(obj);
        return getInternalTypeIdentifierForType(handler);
    }

    public static String getInternalTypeIdentifierForType(TypeHandler handler) {
        if(handler.isPrimitive()) {
            return "primitive(" + handler.getTypeIdentifier() + ")";
        } else {
            return "user_defined(" + handler + ")";
        }
    }

    public static TypeHandler getTypeHandlerTypeHandler() {
        return TYPE_HANDLER_TYPE_HANDLER;
    }

    public static TypeHandler getStaticHandlerForObject(Object obj) {
        TypeHandler handler = getHandlerForObject(obj);
        if(handler != obj) return handler;
        if(handler.isStaticHandler()) {
            return getTypeHandlerTypeHandler();
        }
        return handler.getStaticHandler();
    }
}
