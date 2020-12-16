package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.GlobalSymbolContext;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodTable;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.util.logger.Debug;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class DefaultLibraryProvider {
    public static void populateViaReflection(GlobalSymbolContext globalCtx, PrismarineTypeSystem typeSystem) {
        try {
            globalCtx.put(new Symbol("isInstance", SymbolVisibility.GLOBAL, nativeMethodsToFunction(typeSystem, globalCtx, DefaultLibraryProvider.class.getMethod("isInstance", Object.class, String.class, ISymbolContext.class))));
            globalCtx.put(new Symbol("typeOf", SymbolVisibility.GLOBAL, nativeMethodsToFunction(typeSystem, globalCtx, DefaultLibraryProvider.class.getMethod("typeOf", Object.class, ISymbolContext.class))));
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
        globalCtx.put(new Symbol("type_definition", SymbolVisibility.GLOBAL, typeSystem.getMetaTypeHandler()));

        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Block.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Character.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Debug.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.File.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Integer.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Item.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.JSON.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Math.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.MinecraftTypes.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Project.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Random.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Real.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Reflection.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Tags.class);
        populateViaReflection(typeSystem, globalCtx, com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.Text.class);
    }

    public static boolean isInstance(@NativeFunctionAnnotations.NullableArg Object obj, String typeName, ISymbolContext ctx) {
        typeName = typeName.trim();
        TypeHandler handler = ctx.getTypeSystem().getPrimitiveHandlerForShorthand(typeName);
        if(obj == null) return "null".equals(typeName);
        if(handler == null) {
            throw new IllegalArgumentException("Illegal primitive data type name '" + typeName + "'");
        }
        return handler.isInstance(obj);
    }

    public static String typeOf(@NativeFunctionAnnotations.NullableArg Object obj, ISymbolContext ctx) {
        return ctx.getTypeSystem().getTypeIdentifierForObject(obj);
    }

    private static void populateViaReflection(PrismarineTypeSystem typeSystem, GlobalSymbolContext globalCtx, Class _class) {
        CustomClass tridentClass = createCustomClassFromNative(globalCtx, _class);
        Symbol symbol = new Symbol(_class.getSimpleName(), SymbolVisibility.GLOBAL, tridentClass);
        symbol.setFinalAndLock();
        globalCtx.put(symbol);

        typeSystem.registerUserDefinedType(tridentClass);
    }

    private static CustomClass createCustomClassFromNative(ISymbolContext parentContext, Class cls) {
        CustomClass tridentClass = new CustomClass(cls.getSimpleName(), "trident-util:native", parentContext);
        boolean allStatic = true;

        ClassMethodTable instanceMethods = tridentClass.getInstanceMethods();
        ClassMethodFamily constructors = null;

        Method creationCallback = null;

        for(Method method : cls.getDeclaredMethods()) {
            if(((method.getModifiers() & Modifier.PRIVATE) != 0) || method.getAnnotation(HideFromCustomClass.class) != null) continue;
            if(method.getName().equals("__new")) {
                //constructor
                allStatic = false;
                if(constructors == null) constructors = tridentClass.createConstructorFamily();
                constructors.putOverload(new ClassMethod(tridentClass, null, nativeMethodsToFunction(parentContext.getTypeSystem(), tridentClass.getInnerStaticContext(), method)).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            } else if(method.getName().equals("__onClassCreation")) {
                creationCallback = method;
            } else if(method.getParameterCount() >= 1 && method.getParameters()[0].isAnnotationPresent(NativeFunctionAnnotations.ThisArg.class)) {
                //instance method
                instanceMethods.put(new ClassMethod(tridentClass, null, nativeMethodsToFunction(parentContext.getTypeSystem(), tridentClass.getInnerStaticContext(), method)).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
                allStatic = false;
            } else {
                //static method
                tridentClass.putStaticFunction(nativeMethodsToFunction(parentContext.getTypeSystem(), tridentClass.getInnerStaticContext(), method));
            }
        }

        try {
            for(Field field : cls.getDeclaredFields()) {
                if(((field.getModifiers() & Modifier.PRIVATE) != 0) || field.getAnnotation(HideFromCustomClass.class) != null) continue;
                if((field.getModifiers() & Modifier.STATIC) != 0) {
                    if((field.getModifiers() & Modifier.FINAL) != 0) {
                        tridentClass.putStaticFinalMember(field.getName(), field.get(null));
                    } else {
                        tridentClass.putStaticMember(field.getName(), new Symbol(field.getName(), SymbolVisibility.PUBLIC, field.get(null)));
                    }
                } else {
                    Debug.log("Could not add instance field to via-reflection class " + cls.getSimpleName());
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        for(Class innerClass : cls.getDeclaredClasses()) {
            if(
                    (innerClass.getModifiers() & Modifier.STATIC) == 0 ||
                            ((innerClass.getModifiers() & Modifier.PRIVATE) != 0) ||
                            innerClass.getAnnotation(HideFromCustomClass.class) != null
            ) continue;
            CustomClass innerTridentClass = createCustomClassFromNative(tridentClass.getInnerStaticContext(), innerClass);
            tridentClass.putStaticFinalMember(innerClass.getSimpleName(), innerTridentClass);
        }

        if(allStatic) {
            tridentClass.seal();
        }

        if(creationCallback != null) {
            try {
                creationCallback.invoke(null, tridentClass);
            } catch (IllegalAccessException | InvocationTargetException x) {
                x.printStackTrace();
            }
        }

        return tridentClass;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
    public @interface HideFromCustomClass {}
}
