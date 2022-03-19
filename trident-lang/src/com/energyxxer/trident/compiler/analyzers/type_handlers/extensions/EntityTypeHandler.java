package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.NativeMethodWrapper;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.trident.sets.MinecraftLiteralSet;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class EntityTypeHandler implements TypeHandler<Entity> {
    private ClassMethodFamily constructorFamily;
    private TypeHandlerMemberCollection<Entity> members;

    public CustomClassObject SELECTOR_BASE_NEAREST_PLAYER;
    public CustomClassObject SELECTOR_BASE_ALL_ENTITIES;
    public CustomClassObject SELECTOR_BASE_ALL_PLAYERS;
    public CustomClassObject SELECTOR_BASE_RANDOM;
    public CustomClassObject SELECTOR_BASE_SENDER;
    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        constructorFamily = new ClassMethodFamily("new");
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        try {
            members.putMethod(EntityTypeHandler.class.getMethod("isPlayerName", Entity.class));
            members.putMethod(EntityTypeHandler.class.getMethod("isSelector", Entity.class));
            members.putMethod(Entity.class.getMethod("isPlayer"));
            members.putMethod(Entity.class.getMethod("getLimit"));

            members.putMethod(EntityTypeHandler.class.getMethod("getBase", Entity.class, TokenPattern.class, ISymbolContext.class));
            members.putMethod(EntityTypeHandler.class.getMethod("deriveBase", CustomClassObject.class, Entity.class, TokenPattern.class, ISymbolContext.class));

            ClassMethodFamily getArgumentsFamily = new ClassMethodFamily("getArguments");
            members.put(getArgumentsFamily.getName(), new NativeMethodWrapper<>(getArgumentsFamily));
            getArgumentsFamily.setUseExternalThis(true);
            getArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("getArguments", Entity.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            getArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("getArguments", Entity.class, String.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            getArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("getArguments", Entity.class, TypeHandler.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);

            ClassMethodFamily withArgumentsFamily = new ClassMethodFamily("withArguments");
            members.put(withArgumentsFamily.getName(), new NativeMethodWrapper<>(withArgumentsFamily));
            withArgumentsFamily.setUseExternalThis(true);

            withArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("withArguments", Entity.class, ListObject.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            withArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("withArguments", Entity.class, SelectorArgument.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);

            ClassMethodFamily withoutArgumentsFamily = new ClassMethodFamily("withoutArguments");
            members.put(withoutArgumentsFamily.getName(), new NativeMethodWrapper<>(withoutArgumentsFamily));
            withoutArgumentsFamily.setUseExternalThis(true);

            withoutArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("withoutArguments", Entity.class, String.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            withoutArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("withoutArguments", Entity.class, ListObject.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            withoutArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("withoutArguments", Entity.class, SelectorArgument.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);
            withoutArgumentsFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, globalCtx, EntityTypeHandler.class.getMethod("withoutArguments", Entity.class, TypeHandler.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, globalCtx);

            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, null, EntityTypeHandler.class.getMethod("constructSelector", CustomClassObject.class, ListObject.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
            constructorFamily.putOverload(new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(this.typeSystem, null, EntityTypeHandler.class.getMethod("constructSelector", CustomClassObject.class, SelectorArgument.class, TokenPattern.class, ISymbolContext.class))).setVisibility(SymbolVisibility.PUBLIC), CustomClass.MemberParentMode.FORCE, null, null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        this.typeSystem.registerUserDefinedTypeListener("trident-util:native@SelectorBase", customClass -> {
            SELECTOR_BASE_NEAREST_PLAYER = (CustomClassObject) ((CustomClass) customClass).forceGetMember("NEAREST_PLAYER");
            SELECTOR_BASE_ALL_ENTITIES = (CustomClassObject) ((CustomClass) customClass).forceGetMember("ALL_ENTITIES");
            SELECTOR_BASE_ALL_PLAYERS = (CustomClassObject) ((CustomClass) customClass).forceGetMember("ALL_PLAYERS");
            SELECTOR_BASE_RANDOM = (CustomClassObject) ((CustomClass) customClass).forceGetMember("RANDOM");
            SELECTOR_BASE_SENDER = (CustomClassObject) ((CustomClass) customClass).forceGetMember("SENDER");
        });
    }

    private final PrismarineTypeSystem typeSystem;

    public EntityTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(Entity object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(Entity object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Entity object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<Entity> getHandledClass() {
        return Entity.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "entity";
    }

    public static boolean isPlayerName(@NativeFunctionAnnotations.ThisArg Entity entity) {
        return entity instanceof PlayerName;
    }

    public static boolean isSelector(@NativeFunctionAnnotations.ThisArg Entity entity) {
        return entity instanceof Selector;
    }

    @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@SelectorBase")
    public static CustomClassObject getBase(@NativeFunctionAnnotations.ThisArg Entity entity, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            EntityTypeHandler staticHandler = ctx.getTypeSystem().getHandlerForHandlerClass(EntityTypeHandler.class);
            switch (((Selector) entity).getBase()) {
                case NEAREST_PLAYER: return staticHandler.SELECTOR_BASE_NEAREST_PLAYER;
                case ALL_ENTITIES: return staticHandler.SELECTOR_BASE_ALL_ENTITIES;
                case ALL_PLAYERS: return staticHandler.SELECTOR_BASE_ALL_PLAYERS;
                case RANDOM_PLAYER: return staticHandler.SELECTOR_BASE_RANDOM;
                case SENDER: return staticHandler.SELECTOR_BASE_SENDER;
                default: throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "I thought I covered all my bases!", p, ctx);
            }
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static Entity deriveBase(@NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@SelectorBase") CustomClassObject base, @NativeFunctionAnnotations.ThisArg Entity entity, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            Selector.BaseSelector newBase = Selector.BaseSelector.getForHeader((String) base.forceGetMember("header"));

            Selector newSelector = new Selector(newBase);
            newSelector.addArguments(((Selector) entity).clone().getAllArguments());

            return newSelector;
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static ListObject getArguments(@NativeFunctionAnnotations.ThisArg Entity entity, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            return new ListObject(
                    ctx.getTypeSystem(),
                    ((Selector) entity).getAllArguments()
            );
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static ListObject getArguments(@NativeFunctionAnnotations.ThisArg Entity entity, String key, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            return new ListObject(
                    ctx.getTypeSystem(),
                    ((Selector) entity).getArgumentsByKey(key)
            );
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static ListObject getArguments(@NativeFunctionAnnotations.ThisArg Entity entity, TypeHandler type, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            ListObject list = new ListObject(ctx.getTypeSystem());
            for(SelectorArgument arg : ((Selector) entity).getAllArguments()) {
                if(type.isInstance(arg)) list.add(arg);
            }
            return list;
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static Entity withArguments(@NativeFunctionAnnotations.ThisArg Entity entity, SelectorArgument args, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            Selector newSelector = ((Selector) entity).clone();
            MinecraftLiteralSet.dumpSelectorArguments(newSelector, args, p, ctx);
            return newSelector;
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static Entity withArguments(@NativeFunctionAnnotations.ThisArg Entity entity, ListObject args, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            Selector newSelector = ((Selector) entity).clone();
            MinecraftLiteralSet.dumpSelectorArguments(newSelector, args, p, ctx);
            return newSelector;
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static Entity withoutArguments(@NativeFunctionAnnotations.ThisArg Entity entity, SelectorArgument args, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            Selector newSelector = ((Selector) entity).clone();
            newSelector.removeArgument(args);
            return newSelector;
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static Entity withoutArguments(@NativeFunctionAnnotations.ThisArg Entity entity, String key, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            Selector newSelector = ((Selector) entity).clone();
            newSelector.removeArguments(key);
            return newSelector;
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static Entity withoutArguments(@NativeFunctionAnnotations.ThisArg Entity entity, ListObject args, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            Selector newSelector = ((Selector) entity).clone();
            for(Object rawArg : args) {
                if(rawArg instanceof SelectorArgument) {
                    newSelector.removeArgument((SelectorArgument) rawArg);
                }
            }
            return newSelector;
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static Entity withoutArguments(@NativeFunctionAnnotations.ThisArg Entity entity, TypeHandler type, TokenPattern<?> p, ISymbolContext ctx) {
        if(entity instanceof Selector) {
            Selector newSelector = ((Selector) entity).clone();
            for(SelectorArgument arg : ((Selector) entity).getAllArguments()) {
                if(type.isInstance(arg)) newSelector.removeArgument(arg);
            }
            return newSelector;
        } else {
            throw new IllegalArgumentException("This is not a selector!");
        }
    }

    public static Entity constructSelector(@NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@SelectorBase") CustomClassObject base, @NativeFunctionAnnotations.NullableArg ListObject args, TokenPattern<?> p, ISymbolContext ctx) {
        Selector newSelector = new Selector(Selector.BaseSelector.getForHeader((String) base.forceGetMember("header")));
        MinecraftLiteralSet.dumpSelectorArguments(newSelector, args, p, ctx);
        return newSelector;
    }

    public static Entity constructSelector(@NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@SelectorBase") CustomClassObject base, SelectorArgument args, TokenPattern<?> p, ISymbolContext ctx) {
        Selector newSelector = new Selector(Selector.BaseSelector.getForHeader((String) base.forceGetMember("header")));
        MinecraftLiteralSet.dumpSelectorArguments(newSelector, args, p, ctx);
        return newSelector;
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return constructorFamily;
    }
}
