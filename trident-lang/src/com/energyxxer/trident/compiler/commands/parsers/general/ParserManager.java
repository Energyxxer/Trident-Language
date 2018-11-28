package com.energyxxer.trident.compiler.commands.parsers.general;

import com.energyxxer.trident.compiler.commands.RawCommand;
import com.energyxxer.util.logger.Debug;
import org.reflections.Reflections;

import java.util.HashMap;

public class ParserManager {
    private static HashMap<Class, HashMap<String, Object>> groups = new HashMap<>();

    static {

        Debug.log("Starting command parser caching");
        Reflections r = new Reflections(RawCommand.class.getPackage());
        for(Class<?> cls : r.getTypesAnnotatedWith(ParserGroup.class, true)) {
            if(!cls.isInterface()) {
                Debug.log("Class marked with ParserGroup is not an interface: '" + cls.getSimpleName() + "'", Debug.MessageType.WARN);
            } else {
                groups.put(cls, new HashMap<>());
                Debug.log("Registered parser group '" + cls.getSimpleName() + "'");
            }
        }
        for(Class<?> cls : r.getTypesAnnotatedWith(ParserMember.class)) {
            try {
                boolean hasGroup = false;
                for(Class<?> interf : cls.getInterfaces()) {
                    HashMap<String, Object> toPut = groups.get(interf);
                    if(toPut != null) {
                        hasGroup = true;
                        String key = cls.getAnnotation(ParserMember.class).key();
                        Object parser = cls.newInstance();
                        toPut.put(key, parser);
                        Debug.log("Registered '" + interf.getSimpleName() + "' member '" + cls.getSimpleName() + "'");
                    }
                }
                if(!hasGroup) {
                    Debug.log("ParserMember '" + cls + "' doesn't implement any parser group interface", Debug.MessageType.WARN);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        Debug.log("Finished command parser caching");
    }

    @SuppressWarnings("unchecked")
    public static <T> T getParser(Class<T> group, String key) {
        HashMap<String, Object> innerMap = groups.get(group);
        if(innerMap == null) throw new IllegalArgumentException("No such parser group '" + group.getSimpleName() + "'");
        return (T) innerMap.get(key);
    }

    public static void initialize() {}
}
