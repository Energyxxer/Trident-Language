package com.energyxxer.trident.compiler.analyzers.general;

import com.energyxxer.trident.compiler.analyzers.commands.CommandParser;
import com.energyxxer.trident.compiler.analyzers.constructs.selectors.SelectorArgumentParser;
import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.analyzers.instructions.Instruction;
import com.energyxxer.trident.compiler.analyzers.modifiers.ModifierParser;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria.ScoreEventCriteriaHandler;
import com.energyxxer.util.logger.Debug;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class AnalyzerManager {
    private static HashMap<Class, HashMap<String, Object>> groups = new HashMap<>();

    static {
        Debug.log("Starting analyzer caching");
        long start = System.currentTimeMillis();

        try {
            cacheGroup(CommandParser.class);
            cacheGroup(DefaultLibraryProvider.class);
            cacheGroup(Instruction.class);
            cacheGroup(ModifierParser.class);
            cacheGroup(ScoreEventCriteriaHandler.class);
            cacheGroup(SelectorArgumentParser.class);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }

        long time = System.currentTimeMillis() - start;
        Debug.log("Finished analyzer caching in " + time + " ms");
    }

    private static void cacheGroup(Class<?> groupClass) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        HashMap<String, Object> map;
        if(!groupClass.isInterface()) {
            Debug.log("Class marked with AnalyzerGroup is not an interface: '" + groupClass.getSimpleName() + "'", Debug.MessageType.WARN);
            return;
        } else {
            map = new HashMap<>();
            groups.put(groupClass, map);
            Debug.log("Defined analyzer group '" + groupClass.getSimpleName() + "'");
        }

        String[] classes = groupClass.getAnnotation(AnalyzerGroup.class).classes().split(",");
        for(String simpleClassName : classes) {
            String className = groupClass.getPackage().getName() + "." + simpleClassName;
            Class<?> memberClass = groupClass.getClassLoader().loadClass(className);

            if(!cacheMember(memberClass, groupClass, map)) {
                Debug.log("Class '" + memberClass + "' is not a member of group '" + groupClass + "'!", Debug.MessageType.WARN);
            }
        }
    }

    private static boolean cacheMember(Class<?> memberClass, Class<?> groupClass, HashMap<String, Object> map) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        boolean throwError = true;
        AnalyzerMember annot = memberClass.getAnnotation(AnalyzerMember.class);
        if(annot != null) {
            String key = annot.key();
            Object parser = memberClass.getConstructor().newInstance();
            map.put(key, parser);
            throwError = false;
        }


        for(Class<?> innerClass : memberClass.getDeclaredClasses()) {
            if(groupClass.isAssignableFrom(innerClass)) {
                if(cacheMember(innerClass, groupClass, map)) {
                    throwError = false;
                }
            }
        }
        return !throwError;
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
