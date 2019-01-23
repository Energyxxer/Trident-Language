package com.energyxxer.trident.compiler.analyzers.general;

import com.energyxxer.util.logger.Debug;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class AnalyzerManager {
    private static HashMap<Class, HashMap<String, Object>> groups = new HashMap<>();

    static {
        Debug.log("Starting analyzer caching");
        long start = System.currentTimeMillis();

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addUrls(ClasspathHelper.forJavaClassPath());
        Reflections r = new Reflections(builder);

        for(Class<?> cls : r.getTypesAnnotatedWith(AnalyzerGroup.class, true)) {
            if(!cls.isInterface()) {
                Debug.log("Class marked with AnalyzerGroup is not an interface: '" + cls.getSimpleName() + "'", Debug.MessageType.WARN);
            } else {
                groups.put(cls, new HashMap<>());
                Debug.log("Defined analyzer group '" + cls.getSimpleName() + "'");
            }
        }
        for(Class<?> cls : r.getTypesAnnotatedWith(AnalyzerMember.class)) {
            try {
                boolean hasGroup = false;
                for(Class<?> interf : getInterfaces(cls)) {
                    HashMap<String, Object> toPut = groups.get(interf);
                    if(toPut != null) {
                        hasGroup = true;
                        String key = cls.getAnnotation(AnalyzerMember.class).key();
                        Object parser = cls.getConstructor().newInstance();
                        toPut.put(key, parser);
                        //Debug.log("Defined '" + interf.getSimpleName() + "' member '" + cls.getSimpleName() + "'");
                    }
                }
                if(!hasGroup) {
                    Debug.log("AnalyzerMember '" + cls + "' doesn't implement any analyzer group interface", Debug.MessageType.WARN);
                }
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        long time = System.currentTimeMillis() - start;
        Debug.log("Finished analyzer caching in " + time + " ms");
    }

    private static Collection<Class> getInterfaces(Class cls) {
        ArrayList<Class> interfaces = new ArrayList<>();
        for(Class<?> interf : cls.getInterfaces()) {
            interfaces.add(interf);
            interfaces.addAll(getInterfaces(interf));
        }
        return interfaces;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnalyzer(Class<T> group, String key) {
        HashMap<String, Object> innerMap = groups.get(group);
        if(innerMap == null) throw new IllegalArgumentException("No such analyzer group '" + group.getSimpleName() + "'");
        return (T) innerMap.get(key);
    }

    public static void initialize() {}

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> getAllParsers(Class<T> group) {
        HashMap<String, Object> innerMap = groups.get(group);
        if(innerMap == null) throw new IllegalArgumentException("No such analyzer group '" + group.getSimpleName() + "'");
        return (Collection<T>) innerMap.values();
    }
}
