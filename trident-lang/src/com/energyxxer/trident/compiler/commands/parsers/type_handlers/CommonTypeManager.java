package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

public class CommonTypeManager {
    public Object sanitizeObject(Object obj) {
        if(obj.getClass().isArray()) {
            return new ListType((Object[]) obj);
        }
        return obj;
    }
}
