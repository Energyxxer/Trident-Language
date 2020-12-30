package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.operators.*;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.*;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.*;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunctionBranch;
import com.energyxxer.trident.compiler.lexer.TridentOperatorPool;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ClassMethodSymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.compiler.util.TridentTempFindABetterHome;
import com.energyxxer.trident.sets.DataStructureLiteralSet;
import com.energyxxer.trident.sets.trident.instructions.VariableInstruction;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class CustomClass implements TypeHandler<CustomClass>, ParameterizedMemberHolder {

    private static final HashMap<String, ArrayList<Consumer<CustomClass>>> stringIdentifiedClassListeners = new HashMap<>();

    public enum MemberParentMode {
        CREATE, OVERRIDE, FORCE, INHERIT
    }

    private final PrismarineTypeSystem typeSystem;

    private boolean complete = false;

    private final String name;
    private final HashMap<String, Symbol> staticMembers = new HashMap<>();
    private final LinkedHashMap<String, InstanceMemberSupplier> instanceMemberSuppliers = new LinkedHashMap<>();
    private ArrayList<CustomClass> superClasses;
    private Set<CustomClass> inheritanceTree = null;
    final LinkedHashMap<TypeHandler, PrismarineFunction> explicitCasts = new LinkedHashMap<>();
    final LinkedHashMap<TypeHandler, PrismarineFunction> implicitCasts = new LinkedHashMap<>();

    private boolean _final = false;
    private boolean _static = false;

    private final ClassMethodTable staticMethods = new ClassMethodTable(this);
    final ClassMethodTable instanceMethods = new ClassMethodTable(this);
    private final ClassIndexerFamily indexers = new ClassIndexerFamily();

    private ClassMethodFamily constructorFamily = null;

    private TridentFile definitionFile;
    private ISymbolContext definitionContext;

    private String typeIdentifier;

    private ClassMethodSymbolContext innerStaticContext;

    private String[] typeParamNames = null;
    private GenericSupplier inheritedGenericSuppliers = null;

    //Constructor exclusively for base class
    private CustomClass(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
        this.name = "BASE CLASS";
        this.definitionContext = null;
        this.definitionFile = null;
        this.innerStaticContext = null;
        this.typeIdentifier = "<base class>";
        this.complete = true;

        try {
            ClassMethod baseToStringMethod = new ClassMethod(this, null, nativeMethodsToFunction(typeSystem, null, "toString", CustomClass.class.getMethod("defaultToString", CustomClassObject.class)));
            baseToStringMethod.setVisibility(SymbolVisibility.PUBLIC);
            this.instanceMethods.put(baseToStringMethod, MemberParentMode.FORCE, null, null);

            ClassMethod baseGetIteratorMethod = new ClassMethod(this, null, nativeMethodsToFunction(typeSystem, null, "getIterator", CustomClass.class.getMethod("getIterator", CustomClassObject.class)));
            baseGetIteratorMethod.setVisibility(SymbolVisibility.PUBLIC);
            this.instanceMethods.put(baseGetIteratorMethod, MemberParentMode.FORCE, null, null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static @NativeFunctionAnnotations.NotNullReturn
    String defaultToString(@NativeFunctionAnnotations.ThisArg CustomClassObject obj) {
        return obj.toString();
    }

    public static Object getIterator(@NativeFunctionAnnotations.ThisArg CustomClassObject obj) {
        return null;
    }

    private CustomClass(PrismarineTypeSystem typeSystem, String name) {
        this.typeSystem = typeSystem;
        this.name = name;
    }

    //Constructor exclusively for native classes:
    public CustomClass(String name, String location, ISymbolContext ctx) {
        this.typeSystem = ctx.getTypeSystem();
        this.name = name;
        this.definitionContext = ctx;
        this.definitionFile = (TridentFile) ctx.getStaticParentUnit();
        this.innerStaticContext = new ClassMethodSymbolContext(ctx, null);

        this.typeIdentifier = location + "@" + name;

        this.complete = true;
    }

    public static void defineClass(TokenPattern<?> pattern, ISymbolContext ctx) {
        SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, SymbolVisibility.GLOBAL);
        String className = pattern.find("CLASS_NAME").flatten(false);

        TokenList bodyEntryList = (TokenList) pattern.find("CLASS_DECLARATION_BODY.CLASS_BODY_ENTRIES");
        boolean isCompleteDefinition = pattern.find("CLASS_DECLARATION_BODY") != null;

        VariableInstruction.SymbolModifierMap modifierMap = VariableInstruction.SymbolModifierMap.createFromList(((TokenList) pattern.find("SYMBOL_MODIFIER_LIST")), ctx);
        boolean _static = modifierMap.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC);
        boolean _final = modifierMap.hasModifier(TridentTempFindABetterHome.SymbolModifier.FINAL);

        CustomClass classObject = ((TridentFile) ctx.getStaticParentUnit()).getClassForName(className);
        if(classObject != null) {
            if(classObject.isComplete()) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot modify definition of class '" + classObject.getTypeIdentifier() + "': Class is already complete.", pattern.find("CLASS_NAME"), ctx);
            }
        } else {
            classObject = new CustomClass(ctx.getTypeSystem(), className);
            classObject.definitionFile = (TridentFile) ctx.getStaticParentUnit();
            classObject.definitionContext = ctx;
            classObject.innerStaticContext = new ClassMethodSymbolContext(ctx, null);
            classObject.typeIdentifier = classObject.definitionFile.getResourceLocation() + "@" + classObject.name;
            classObject._static = _static;
            classObject._final = _final;

            ((TridentFile) ctx.getStaticParentUnit()).registerInnerClass(classObject, pattern, ctx);
            Symbol sym = new Symbol(className, visibility, classObject);
            sym.setFinalAndLock();
            ctx.putInContextForVisibility(visibility, sym);
        }
        ctx = classObject.innerStaticContext;

        ArrayList<CustomClass> oldSuperClasses = classObject.superClasses;
        classObject.superClasses = new ArrayList<>();
        classObject.superClasses.add(((TridentTypeSystem) ctx.getTypeSystem()).getBaseClass());

        if(pattern.find("FORMAL_TYPE_PARAMETERS") != null) {
            classObject.typeParamNames = (String[]) pattern.find("FORMAL_TYPE_PARAMETERS").evaluate(ctx);

            GenericContext genericContext = new GenericContext(classObject, classObject.typeParamNames);

            for(int i = 0; i < classObject.typeParamNames.length; i++) {
                GenericStandInType standIn = new GenericStandInType(classObject.typeSystem, genericContext, i);
                classObject.innerStaticContext.put(new Symbol(classObject.typeParamNames[i], TridentSymbolVisibility.PRIVATE, standIn));
            }
        }

        if(pattern.find("CLASS_INHERITS") != null) {
            TokenList inheritsList = ((TokenList) pattern.find("CLASS_INHERITS.SUPERCLASS_LIST"));
            for(TokenPattern<?> rawParent : inheritsList.searchByName("INTERPOLATION_TYPE")) {
                TypeHandler parentType = (TypeHandler) rawParent.evaluate(ctx);
                while(parentType instanceof GenericWrapperType) {
                    if(classObject.inheritedGenericSuppliers == null) classObject.inheritedGenericSuppliers = new GenericSupplier();
                    ((GenericWrapperType) parentType).getGenericSupplier().dumpInto(classObject.inheritedGenericSuppliers);

                    parentType = ((GenericWrapperType) parentType).getSourceType();
                }

                if(parentType instanceof CustomClass) {
                    if(((CustomClass) parentType)._final || ((CustomClass) parentType)._static) {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot inherit a static or final class: " + parentType, rawParent, ctx);
                    }
                    if(classObject.superClasses.contains(parentType)) {
                        throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Duplicated superclass: " + ((CustomClass) parentType).typeIdentifier, rawParent, ctx);
                    } else {
                        classObject.superClasses.add((CustomClass) parentType);
                    }
                } else {
                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "'" + parentType.getTypeIdentifier() + "' is not a class type.", rawParent, ctx);
                }
            }
        }
        if(isCompleteDefinition) {
            if(oldSuperClasses != null && !classObject.superClasses.containsAll(oldSuperClasses)) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Incomplete definition promised to extend " + oldSuperClasses + "; not all inherited in the complete definition.", pattern.tryFind("CLASS_INHERITS"), ctx);
            }

            if(_final != classObject._final) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Incomplete definition promised to make " + classObject + " " + (classObject._final ? "not" : "") + " final; such was not true in the complete definition.", pattern.tryFind("SYMBOL_MODIFIER_LIST"), ctx);
            }
            if(_static != classObject._static) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Incomplete definition promised to make " + classObject + " " + (classObject._static ? "not" : "") + " static; such was not true in the complete definition.", pattern.tryFind("SYMBOL_MODIFIER_LIST"), ctx);
            }

            classObject.complete = true;
            for(CustomClass superClass : classObject.getInheritanceTree()) {
                if(superClass == classObject) continue;
                classObject.instanceMethods.putAll(superClass.instanceMethods, pattern.find("CLASS_NAME"), ctx);
                classObject.indexers.putAll(superClass.indexers, pattern.find("CLASS_NAME"), ctx);
            }
            if(bodyEntryList != null) {
                ArrayList<TokenPattern<?>> staticEntries = new ArrayList<>();

                for(TokenPattern<?> entry : bodyEntryList.getContents()) {
                    entry = ((TokenStructure)entry).getContents();
                    boolean wasStatic = classObject.parseEntry(entry, ctx, false);
                    if(wasStatic) {
                        staticEntries.add(entry);
                    }
                }

                for(TokenPattern<?> entry : staticEntries) {
                    classObject.parseEntry(entry, ctx, true);
                }
            }

            classObject.instanceMethods.checkClashingInheritedMethodsResolved(pattern.find("CLASS_NAME"), ctx);
            classObject.indexers.checkClashingInheritedIndexersResolved(classObject, pattern.find("CLASS_NAME"), ctx);

            for(Symbol field : classObject.staticMembers.values()) {
                if(field.isFinal() && field.maySet()) {
                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Final static symbol '" + field.getName() + "' was not initialized.", pattern, ctx);
                }
            }
            ctx.getTypeSystem().registerUserDefinedType(classObject);
        }

        classObject._final = _final;
    }

    private boolean parseEntry(TokenPattern<?> entry, ISymbolContext ctx, boolean shouldParseStatic) {
        switch(entry.getName()) {
            case "CLASS_MEMBER": {
                VariableInstruction.SymbolDeclaration decl = parseSymbolDeclaration(entry, ctx);
                if(!shouldParseStatic && decl.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)) {
                    return true;
                }
                decl.preParseConstraints();
                MemberParentMode mode = MemberParentMode.CREATE;
                if(entry.find("MEMBER_PARENT_MODE") != null) {
                    mode = MemberParentMode.valueOf(entry.find("MEMBER_PARENT_MODE").flatten(false).toUpperCase());
                }

                if(decl.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)) {
                    if(mode != MemberParentMode.CREATE) {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot " + mode.toString().toLowerCase() + " a static field.", entry.find("MEMBER_PARENT_MODE"), ctx);
                    }
                    Symbol sym = decl.getSupplier().get();

                    if(sym.getTypeConstraints() != null && sym.getTypeConstraints().isGeneric()) {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "A static field cannot be constrained to a type parameter: " + sym.getTypeConstraints(), entry.tryFind("TYPE_CONSTRAINT"), ctx);
                    }
                    this.putStaticMember(decl.getName(), sym);
                } else {
                    if(_static) {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Class " + getTypeIdentifier() + " is static; cannot have instance field.", entry, ctx);
                    }
                    if(mode == MemberParentMode.CREATE) {
                        InstanceMemberSupplier alreadyDefinedSupplier = this.getInstanceMemberSupplier(decl.getName());
                        if(alreadyDefinedSupplier != null && alreadyDefinedSupplier.getDefiningClass() == this) {
                            throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Duplicate member '" + decl.getName() + "': it's already defined in the same class.", entry, ctx);
                        } if(alreadyDefinedSupplier != null) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Member '" + decl.getName() + "' is already defined in inherited class " + alreadyDefinedSupplier.getDefiningClass().typeIdentifier + ". Use the 'override' keyword to change its default value.", entry, ctx);
                        }
                    } else if(mode == MemberParentMode.OVERRIDE) {
                        InstanceMemberSupplier alreadyDefinedSupplier = this.getInstanceMemberSupplier(decl.getName());
                        if(alreadyDefinedSupplier == null) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override field '" + decl.getName() + "': not found in any of the inherited classes.", entry, ctx);
                        } else if(alreadyDefinedSupplier.getDefiningClass() == this) {
                            throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Cannot override member '" + decl.getName() + "': it's already defined in the same class.", entry, ctx);
                        }
                        if(((InstanceFieldSupplier) alreadyDefinedSupplier).getDecl().hasModifier(TridentTempFindABetterHome.SymbolModifier.FINAL)) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override field '" + decl.getName() + "': it's defined as final in " + alreadyDefinedSupplier.getDefiningClass().typeIdentifier, entry, ctx);
                        } else if(decl.hasModifier(TridentTempFindABetterHome.SymbolModifier.FINAL)) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override field '" + decl.getName() + "' with a final field.", entry, ctx);
                        } else if(((InstanceFieldSupplier) alreadyDefinedSupplier).isProperty) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override property '" + decl.getName() + "' with a field.", entry, ctx);
                        }

                        TypeConstraints thisConstraints = decl.getConstraint(null);
                        TypeConstraints otherConstraints = ((InstanceFieldSupplier) alreadyDefinedSupplier).getDecl().getConstraint(null);
                        if (!TypeConstraints.constraintsEqual(thisConstraints, otherConstraints)) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override field '" + decl.getName() + "': Mismatch of type constraints. Defined in superclass " + alreadyDefinedSupplier.getDefiningClass().typeIdentifier + ": " + otherConstraints + "; Found: " + thisConstraints, entry, ctx);
                        }
                    }

                    this.putInstanceMember(decl.getName(), new InstanceFieldSupplier(decl, false) {
                        @Override
                        public String getName() {
                            return decl.getName();
                        }

                        @Override
                        public Symbol constructSymbol(CustomClassObject thiz) {
                            return decl.getSupplier().get();
                        }

                        @Override
                        public SymbolVisibility getVisibility() {
                            return decl.getVisibility();
                        }

                        @Override
                        public CustomClass getDefiningClass() {
                            return CustomClass.this;
                        }
                    });
                }
                break;
            }
            case "CLASS_FUNCTION": {
                VariableInstruction.SymbolModifierMap modifiers = VariableInstruction.SymbolModifierMap.createFromList(((TokenList) entry.find("SYMBOL_MODIFIER_LIST")), ctx);
                if(!shouldParseStatic && modifiers.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)) {
                    return true;
                }
                if(_static && !modifiers.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Class " + getTypeIdentifier() + " is static; cannot have instance methods.", entry.tryFind("SYMBOL_MODIFIER_LIST"), ctx);
                }
                String functionName = entry.find("SYMBOL_NAME").flatten(false);
                boolean isConstructor = "new".equals(functionName);
                MemberParentMode mode = MemberParentMode.CREATE;
                if(entry.find("MEMBER_PARENT_MODE") != null) {
                    mode = MemberParentMode.valueOf(entry.find("MEMBER_PARENT_MODE").flatten(false).toUpperCase());
                }

                if(isConstructor && modifiers.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "'static' modifier not allowed here.", entry.find("SYMBOL_MODIFIER_LIST.LITERAL_STATIC"), this.definitionContext);
                }

                SymbolVisibility memberVisibility = TridentProductions.parseClassMemberVisibility(entry.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL, this);

                PrismarineFunctionBranch branch = TridentTempFindABetterHome.parseDynamicFunction(entry.find("DYNAMIC_FUNCTION"), ctx);

                if(!isConstructor) {
                    ClassMethod method = new ClassMethod(
                            this,
                            entry,
                            new PrismarineFunction(
                                    functionName,
                                    branch,
                                    this.prepareFunctionContext()
                            )
                    ).setModifiers(modifiers).setVisibility(memberVisibility);

                    if(modifiers.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)) {
                        this.staticMethods.put(method, mode, entry, ctx);
                        this.innerStaticContext.putClassFunction(this.staticMethods.getFamily(functionName));
                    } else {
                        this.instanceMethods.put(method, mode, entry, ctx);
                    }
                } else {
                    ClassMethod method = new ClassMethod(
                            this,
                            entry,
                            new PrismarineFunction(
                                    functionName,
                                    branch,
                                    this.prepareFunctionContext()
                            )
                    ).setModifiers(modifiers).setVisibility(memberVisibility);

                    if(this.constructorFamily == null) {
                        this.constructorFamily = new ClassMethodFamily("new");
                    }
                    this.constructorFamily.putOverload(method, mode, entry, ctx);
                }
                break;
            }
            case "CLASS_INDEXER": {
                if(_static) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Class " + getTypeIdentifier() + " is static; cannot have indexers.", entry.tryFind("SYMBOL_MODIFIER_LIST"), ctx);
                }
                SymbolVisibility defaultVisibility = TridentProductions.parseClassMemberVisibility(entry.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL, this);
                MemberParentMode mode = MemberParentMode.CREATE;
                if(entry.find("MEMBER_PARENT_MODE") != null) {
                    mode = MemberParentMode.valueOf(entry.find("MEMBER_PARENT_MODE").flatten(false).toUpperCase());
                }

                FormalParameter indexParam = TridentTempFindABetterHome.createFormalParam(entry.find("FORMAL_PARAMETER"), ctx);

                TridentUserFunctionBranch getterBranch = new TridentUserFunctionBranch(ctx.getTypeSystem(), Collections.singletonList(indexParam), entry.find("CLASS_GETTER.ANONYMOUS_INNER_FUNCTION"), (TypeConstraints) entry.find("CLASS_GETTER.TYPE_CONSTRAINTS").evaluate(ctx));
                SymbolVisibility getterVisibility = TridentProductions.parseClassMemberVisibility(entry.find("CLASS_GETTER.SYMBOL_VISIBILITY"), defaultVisibility, this);
                PrismarineFunction getter = new PrismarineFunction(
                        "<indexer getter>",
                        getterBranch,
                        this.prepareFunctionContext()
                );

                PrismarineFunction setter = null;
                SymbolVisibility setterVisibility = TridentSymbolVisibility.LOCAL;
                if(entry.find("CLASS_SETTER") != null) {
                    setterVisibility = TridentProductions.parseClassMemberVisibility(entry.find("CLASS_SETTER.SYMBOL_VISIBILITY"), defaultVisibility, this);
                    TridentUserFunctionBranch setterBranch = new TridentUserFunctionBranch(ctx.getTypeSystem(), Arrays.asList(indexParam, TridentTempFindABetterHome.createFormalParam(entry.find("CLASS_SETTER.FORMAL_PARAMETER"), ctx)), entry.find("CLASS_SETTER.ANONYMOUS_INNER_FUNCTION"), null);
                    setter = new PrismarineFunction(
                            "<indexer setter>",
                            setterBranch,
                            this.prepareFunctionContext()
                    );
                }

                if(getterVisibility.getVisibilityIndex() > defaultVisibility.getVisibilityIndex()) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Getter access privileges must not be more accessible than the indexer's. Indexer access: '" + defaultVisibility.toString().toLowerCase() + "', Getter access: " + getterVisibility.toString().toLowerCase(), entry.tryFind("CLASS_GETTER.SYMBOL_VISIBILITY"), ctx);
                }
                if(setter != null && setterVisibility.getVisibilityIndex() > defaultVisibility.getVisibilityIndex()) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Setter access privileges must not be more accessible than the indexer's. Indexer access: '" + defaultVisibility.toString().toLowerCase() + "', Setter access: " + setterVisibility.toString().toLowerCase(), entry.tryFind("CLASS_SETTER.SYMBOL_VISIBILITY"), ctx);
                }

                ClassIndexer indexer = new ClassIndexer(this, entry, indexParam, getter, setter);
                indexer.setGetterVisibility(getterVisibility);
                indexer.setSetterVisibility(setterVisibility);

                this.indexers.put(indexer, mode, entry, ctx);
                break;
            }
            case "CLASS_PROPERTY": {
                VariableInstruction.SymbolModifierMap modifiers = VariableInstruction.SymbolModifierMap.createFromList(((TokenList) entry.find("SYMBOL_MODIFIER_LIST")), ctx);
                if(!shouldParseStatic && modifiers.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)) {
                    return true;
                }
                MemberParentMode mode = MemberParentMode.CREATE;
                if(entry.find("MEMBER_PARENT_MODE") != null) {
                    mode = MemberParentMode.valueOf(entry.find("MEMBER_PARENT_MODE").flatten(false).toUpperCase());
                }
                String propertyName = entry.find("SYMBOL_NAME").flatten(false);
                SymbolVisibility defaultVisibility = TridentProductions.parseClassMemberVisibility(entry.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL, this);

                TridentUserFunctionBranch getterBranch = new TridentUserFunctionBranch(ctx.getTypeSystem(), Collections.emptyList(), entry.find("CLASS_GETTER.ANONYMOUS_INNER_FUNCTION"), (TypeConstraints) entry.find("CLASS_GETTER.TYPE_CONSTRAINTS").evaluate(ctx));
                SymbolVisibility getterVisibility = TridentProductions.parseClassMemberVisibility(entry.find("CLASS_GETTER.SYMBOL_VISIBILITY"), defaultVisibility, this);
                PrismarineFunction getter = new PrismarineFunction(
                        "<property getter>",
                        getterBranch,
                        this.prepareFunctionContext()
                );

                PrismarineFunction setter = null;
                SymbolVisibility setterVisibility = TridentSymbolVisibility.LOCAL;
                if(entry.find("CLASS_SETTER") != null) {
                    setterVisibility = TridentProductions.parseClassMemberVisibility(entry.find("CLASS_SETTER.SYMBOL_VISIBILITY"), defaultVisibility, this);
                    TridentUserFunctionBranch setterBranch = new TridentUserFunctionBranch(ctx.getTypeSystem(), Collections.singletonList(TridentTempFindABetterHome.createFormalParam(entry.find("CLASS_SETTER.FORMAL_PARAMETER"), ctx)), entry.find("CLASS_SETTER.ANONYMOUS_INNER_FUNCTION"), new TypeConstraints(typeSystem, (TypeHandler<?>) null, true));
                    setter = new PrismarineFunction(
                            "<property setter>",
                            setterBranch,
                            this.prepareFunctionContext()
                    );
                }

                if(getterVisibility.getVisibilityIndex() > defaultVisibility.getVisibilityIndex()) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Getter access privileges must not be more accessible than the property's. Property access: '" + defaultVisibility.toString().toLowerCase() + "', Getter access: " + getterVisibility.toString().toLowerCase(), entry.tryFind("CLASS_GETTER.SYMBOL_VISIBILITY"), ctx);
                }
                if(setter != null && setterVisibility.getVisibilityIndex() > defaultVisibility.getVisibilityIndex()) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Setter access privileges must not be more accessible than the property's. Property access: '" + defaultVisibility.toString().toLowerCase() + "', Setter access: " + setterVisibility.toString().toLowerCase(), entry.tryFind("CLASS_SETTER.SYMBOL_VISIBILITY"), ctx);
                }

                ClassProperty property = new ClassProperty(this, entry, propertyName, getter, setter);
                property.setGetterVisibility(getterVisibility);
                property.setSetterVisibility(setterVisibility);

                if(modifiers.hasModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)) {
                    if(mode != MemberParentMode.CREATE) {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot " + mode.toString().toLowerCase() + " a static property.", entry.find("MEMBER_PARENT_MODE"), ctx);
                    }
                    Symbol sym = property.createSymbol(null);
                    if(sym.getTypeConstraints() != null && sym.getTypeConstraints().isGeneric()) {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "A static property cannot be constrained to a type parameter: " + sym.getTypeConstraints(), entry.tryFind("TYPE_CONSTRAINT"), ctx);
                    }
                    this.putStaticMember(propertyName, sym);
                } else {
                    if(_static) {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Class " + getTypeIdentifier() + " is static; cannot have properties.", entry.tryFind("SYMBOL_MODIFIER_LIST"), ctx);
                    }
                    VariableInstruction.SymbolDeclaration decl = new VariableInstruction.SymbolDeclaration(propertyName);
                    decl.setVisibility(defaultVisibility);

                    if(mode == MemberParentMode.CREATE) {
                        InstanceMemberSupplier alreadyDefinedSupplier = this.getInstanceMemberSupplier(propertyName);
                        if(alreadyDefinedSupplier != null && alreadyDefinedSupplier.getDefiningClass() == this) {
                            throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Duplicate member '" + propertyName + "': it's already defined in the same class.", entry, ctx);
                        } if(alreadyDefinedSupplier != null) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Member '" + propertyName + "' is already defined in inherited class " + alreadyDefinedSupplier.getDefiningClass().typeIdentifier + ". Use the 'override' keyword to change its default value.", entry, ctx);
                        }
                    } else if(mode == MemberParentMode.OVERRIDE) {
                        InstanceMemberSupplier alreadyDefinedSupplier = this.getInstanceMemberSupplier(propertyName);
                        if(alreadyDefinedSupplier == null) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override property '" + propertyName + "': not found in any of the inherited classes.", entry, ctx);
                        } else if(alreadyDefinedSupplier.getDefiningClass() == this) {
                            throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Cannot override member '" + propertyName + "': it's already defined in the same class.", entry, ctx);
                        } else if(!((InstanceFieldSupplier) alreadyDefinedSupplier).isProperty) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override field '" + propertyName + "' with a property.", entry, ctx);
                        }
//
//                    TypeConstraints thisConstraints = decl.getConstraint(null);
//                    TypeConstraints otherConstraints = ((InstanceFieldSupplier) alreadyDefinedSupplier).getDecl().getConstraint(null);
//                    if (!TypeConstraints.constraintsEqual(thisConstraints, otherConstraints)) {
//                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override member '" + decl.getName() + "': Mismatch of type constraints. Defined in superclass " + alreadyDefinedSupplier.getDefiningClass().typeIdentifier + ": " + otherConstraints + "; Found: " + thisConstraints, entry, ctx);
//                    }
                    }

                    this.putInstanceMember(propertyName, new InstanceFieldSupplier(decl, true) {
                        @Override
                        public CustomClass getDefiningClass() {
                            return CustomClass.this;
                        }

                        @Override
                        public String getName() {
                            return propertyName;
                        }

                        @Override
                        public Symbol constructSymbol(CustomClassObject thiz) {
                            return property.createSymbol(thiz);
                        }

                        @Override
                        public SymbolVisibility getVisibility() {
                            return defaultVisibility;
                        }
                    });
                }

                break;
            }
            case "CLASS_OVERRIDE": {
                if(_static) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Class " + getTypeIdentifier() + " is static; cannot have type conversion definitions.", entry.tryFind("SYMBOL_MODIFIER_LIST"), ctx);
                }
                boolean implicit = "implicit".equals(entry.find("CLASS_TRANSFORM_TYPE").flatten(false));
                TypeHandler toType = (TypeHandler) entry.find("INTERPOLATION_TYPE").evaluate(this.getInnerStaticContext());
                PrismarineFunctionBranch branch = new TridentUserFunctionBranch(ctx.getTypeSystem(), Collections.emptyList(), entry.find("ANONYMOUS_INNER_FUNCTION"), new TypeConstraints(typeSystem, toType, false));
                PrismarineFunction function = new PrismarineFunction(toType.getTypeIdentifier(), branch, this.getInnerStaticContext());
                if(implicit) branch.setShouldCoerceReturn(false);
                LinkedHashMap<TypeHandler, PrismarineFunction> castMap = implicit ? this.implicitCasts : this.explicitCasts;
                castMap.put(toType, function);
                break;
            }
            case "CLASS_OPERATOR": {
                String operatorSymbol = entry.find("OPERATOR_SYMBOL").flatten(false);
                int operandCount = (((TokenList) entry.find("DYNAMIC_FUNCTION.PRE_CODE_BLOCK.FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST")).size() + 1) / 2;
                OperatorPool operatorPool = TridentOperatorPool.INSTANCE;
                Operator associatedOperator;
                if(operandCount == 1) {
                    associatedOperator = operatorPool.getUnaryLeftOperatorForSymbol(operatorSymbol);
                    if(associatedOperator == null) {
                        associatedOperator = operatorPool.getUnaryLeftOperatorForSymbol(operatorSymbol);
                    }
                } else {
                    associatedOperator = operatorPool.getBinaryOrTernaryOperatorForSymbol(operatorSymbol);
                    if((associatedOperator instanceof TernaryOperator) != (operandCount == 3)) {
                        associatedOperator = null;
                    }
                }
                if(associatedOperator == null) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "There is no operator " + operatorSymbol + " for " + operandCount + " operands", entry.find("OPERATOR_PARAMETERS"), ctx);
                }
                PrismarineFunctionBranch branch = TridentTempFindABetterHome.parseDynamicFunction(entry.find("DYNAMIC_FUNCTION"), ctx);
                ClassMethod method = new ClassMethod(
                        this,
                        entry,
                        new PrismarineFunction(
                                operatorSymbol,
                                branch,
                                this.prepareFunctionContext()
                        )
                ).setModifiers(
                        new VariableInstruction.SymbolModifierMap().setModifier(TridentTempFindABetterHome.SymbolModifier.STATIC)
                ).setVisibility(SymbolVisibility.PUBLIC);

                OperatorManager operatorManager = typeSystem.getOperatorManager();
                if(associatedOperator instanceof UnaryOperator) {
                    operatorManager.registerUnaryLeftOperator(operatorSymbol, method);
                } else if(associatedOperator instanceof BinaryOperator) {
                    operatorManager.registerBinaryOperator(operatorSymbol, method);
                } else {
                    operatorManager.registerTernaryOperator(operatorSymbol, method);
                }
                break;
            }
            case "COMMENT": {
                break;
            }
            default: {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + entry.getName() + "'", entry, ctx);
            }
        }
        return false;
    }

    public void putStaticFunction(PrismarineFunction value) {
        ClassMethod method = new ClassMethod(
                this,
                null,
                new PrismarineFunction(
                        value.getFunctionName(),
                        value.getBranch(),
                        this.prepareFunctionContext()
                )
        ).setVisibility(SymbolVisibility.PUBLIC);

        this.staticMethods.put(method, MemberParentMode.FORCE, null, null);
        this.innerStaticContext.putMethod(this.staticMethods.getFamily(value.getFunctionName()));
    }

    public void putStaticFinalMember(String name, Object value) {
        Symbol sym = new Symbol(name, SymbolVisibility.PUBLIC, value);
        sym.setValue(value);
        sym.setFinalAndLock();
        putStaticMember(name, sym);
    }

    public void putStaticMember(String name, Symbol sym) {
        staticMembers.put(name, sym);
        innerStaticContext.put(sym);
    }

    public void putInstanceMember(String name, InstanceMemberSupplier symSupplier) {
        instanceMemberSuppliers.put(name, symSupplier);
    }

    public void seal() {
        this.setFinal(true);
        this.setStatic(true);
    }

    public void setFinal(boolean _final) {
        this._final = _final;
    }

    public void setStatic(boolean _static) {
        this._static = _static;
    }

    public ClassMethodFamily createConstructorFamily() {
        constructorFamily = new ClassMethodFamily("new");
        return constructorFamily;
    }

    public InstanceMemberSupplier getInstanceMemberSupplier(String name) {
        for(CustomClass cls : getInheritanceTree()) {
            InstanceMemberSupplier supplier = cls.instanceMemberSuppliers.get(name);
            if(supplier != null) return supplier;
        }
        return null;
    }

    private ISymbolContext prepareFunctionContext() {
        return new SymbolContext(this.innerStaticContext);
    }

    @Override
    public Object getMember(CustomClass object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        object.assertComplete(pattern, ctx);
        if(object.staticMembers.containsKey(member)) {
            Symbol sym = object.staticMembers.get(member);

            if(object.hasAccess(ctx, sym.getVisibility())) {
                return keepSymbol ? sym : sym.getValue(pattern, ctx);
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "'" + sym.getName() + "' has " + sym.getVisibility().toString().toLowerCase() + " access in " + object.getClassTypeIdentifier(), pattern, ctx);
            }
        }
        return ctx.getTypeSystem().getMetaTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
    }

    @Override
    public Object getMemberForParameters(String memberName, TokenPattern<?> pattern, ActualParameterList params, ISymbolContext ctx, boolean keepSymbol) {
        Object foundClassMethod = staticMethods.find(memberName, params, ctx, null);
        if(foundClassMethod == null) {
            try {
                foundClassMethod = getMember(this, memberName, pattern, ctx, keepSymbol);
            } catch(MemberNotFoundException ignore) {
            }
        }
        if(foundClassMethod == null) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot resolve function or method '" + memberName + "' of " + ctx.getTypeSystem().getTypeIdentifierForObject(this), pattern, ctx);
        }
        return foundClassMethod;
    }

    Set<CustomClass> getInheritanceTree() {
        if(inheritanceTree == null) {
            inheritanceTree = new LinkedHashSet<>();

            Queue<CustomClass> classQueue = new LinkedBlockingQueue<>();
            classQueue.add(this);

            while(!classQueue.isEmpty()) {
                CustomClass other = classQueue.remove();
                inheritanceTree.add(other);
                if(other.superClasses != null) {
                    for(CustomClass next : other.superClasses) {
                        if(!inheritanceTree.contains(next)) {
                            classQueue.add(next);
                        }
                    }
                }
            }
        }
        return inheritanceTree;
    }

    private void assertComplete(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(!isComplete()) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Definition of class '" + getTypeIdentifier() + "' is not yet complete", pattern, ctx);
        }
    }

    @Override
    public Object getIndexer(CustomClass object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return ctx.getTypeSystem().getMetaTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
    }

    @Override
    public Object cast(CustomClass object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return ctx.getTypeSystem().getMetaTypeHandler().cast(object, targetType, pattern, ctx);
    }

    @Override
    public Object coerce(CustomClass object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return ctx.getTypeSystem().getMetaTypeHandler().coerce(object, targetType, pattern, ctx);
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into, ISymbolContext ctx) {
        return ctx.getTypeSystem().getMetaTypeHandler().canCoerce(object, into, ctx);
    }

    @Override
    public boolean isSubType(TypeHandler<?> other) {
        if(!(other instanceof CustomClass)) return false;
        for(CustomClass type : ((CustomClass) other).getInheritanceTree()) {
            if(type == this || type.getTypeIdentifier().equals(this.getTypeIdentifier())) return true;
            //Comparing strings, just in case the class has been duplicated (e.g. native in dependencies)
        }
        return false;
    }

    @Override
    public String toString() {
        return typeIdentifier;
    }

    @Override
    public Class<CustomClass> getHandledClass() {
        return CustomClass.class;
    }

    @Override
    public boolean isSelfHandler() {
        return true;
    }

    @Override
    public boolean isInstance(Object obj) {
        if(!(obj instanceof CustomClassObject)) return false;
        for(CustomClass type : ((CustomClassObject) obj).getType().getInheritanceTree()) {
            if(type == this || type.getTypeIdentifier().equals(this.getTypeIdentifier())) return true;
            //Comparing strings, just in case the class has been duplicated (e.g. native in dependencies)
        }
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    String getClassTypeIdentifier() {
        return typeIdentifier;
    }

    @Override
    public String getTypeIdentifier() {
        return getClassTypeIdentifier();
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        assertComplete(pattern, ctx);
        if(_static) {
            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Class '" + getClassTypeIdentifier() + "' is static; cannot be instantiated.", pattern, ctx);
        }
        if(genericSupplier != null && !isGeneric()) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Class '" + getClassTypeIdentifier() + "' is not generic.", pattern, ctx);
        }
        return (params, ctx2, thisObject) -> {
            CustomClassObject created = new CustomClassObject(this);

            if(genericSupplier != null) {
                created.putGenericInfo(this, genericSupplier.get(this));
            }
            if(inheritedGenericSuppliers != null) {
                created.getOrCreateGenericSupplier().dumpFrom(GenericUtils.resolveStandIns(inheritedGenericSuppliers, created, params, ctx));
            }

            for(CustomClass cls : getInheritanceTree()) {
                for(InstanceMemberSupplier symbolSupplier : cls.instanceMemberSuppliers.values()) {
                    if(!created.containsMember(symbolSupplier.getName())) {
                        Symbol sym = symbolSupplier.constructSymbol(created);
                        if(sym.getTypeConstraints() != null && sym.getTypeConstraints().isGeneric()) {
                            sym.setTypeConstraints(GenericUtils.nonGeneric(sym.getTypeConstraints(), created, params, ctx));
                        }
                        created.putMemberIfAbsent(sym);
                    }
                }
            }

            if(constructorFamily != null) {
                PrismarineFunction.FixedThisFunctionSymbol pickedConstructor = constructorFamily.pickOverloadSymbol(params, ctx, created);
                pickedConstructor.safeCall(params, ctx2);
                for(Symbol field : created.instanceMembers.values()) {
                    if(field.isFinal() && field.maySet()) {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Final symbol '" + field.getName() + "' was not initialized in constructor.", pattern, ctx2);
                    }
                }
            }

            return created;
        };
    }

    public boolean isGeneric() {
        return typeParamNames != null;
    }

    public CustomClassObject forceInstantiate() {
        CustomClassObject created = new CustomClassObject(this);
        for(CustomClass cls : getInheritanceTree()) {
            for(InstanceMemberSupplier symbolSupplier : cls.instanceMemberSuppliers.values()) {
                if(!created.containsMember(symbolSupplier.getName()))
                    created.putMemberIfAbsent(symbolSupplier.constructSymbol(created));
            }
        }
        return created;
    }

    public ISymbolContext getDeclaringContext() {
        return definitionContext;
    }

    public TridentFile getDeclaringFile() {
        return definitionFile;
    }

    public ISymbolContext getInnerStaticContext() {
        return innerStaticContext;
    }

    public boolean hasAccess(ISymbolContext ctx, SymbolVisibility visibility) {
        return visibility.isVisibleFromContext(null, this.innerStaticContext, ctx);
    }

    public boolean isProtectedAncestor(ISymbolContext ctx) {
        for(CustomClass cls : getInheritanceTree()) {
            if(ctx.isAncestor(cls.innerStaticContext)) {
                return true;
            }
        }
        return false;
    }

    public ClassMethodTable getInstanceMethods() {
        return instanceMethods;
    }

    @Override
    public boolean isStaticHandler() {
        return true;
    }

    public boolean isComplete() {
        return complete;
    }

    public String getClassName() {
        return name;
    }

    public ClassIndexer getIndexer() {
        return indexers.get();
    }

    interface InstanceMemberSupplier {
        CustomClass getDefiningClass();
        String getName();
        Symbol constructSymbol(CustomClassObject thiz);

        SymbolVisibility getVisibility();
    }

    private static abstract class InstanceFieldSupplier implements InstanceMemberSupplier {
        private final VariableInstruction.SymbolDeclaration decl;
        private final boolean isProperty;

        public InstanceFieldSupplier(VariableInstruction.SymbolDeclaration decl, boolean isProperty) {
            this.decl = decl;
            this.isProperty = isProperty;
        }

        public VariableInstruction.SymbolDeclaration getDecl() {
            return decl;
        }
    }

    //public PrimitivePrismarineFunction createFunction(CustomClassObject thiz) {
    //    ISymbolContext innerFrame = definingClass.prepareFunctionContext(thiz);
    //    return new PrismarineFunction(functionName, branches, innerFrame, thiz);
    //}

    public Object forceGetMember(String key) {
        return staticMembers.get(key).getValue(null, null);
    }



    public static CustomClass createBaseClass(PrismarineTypeSystem typeSystem) {
        return new CustomClass(typeSystem);
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    public String[] getTypeParamNames() {
        return typeParamNames;
    }



    private VariableInstruction.SymbolDeclaration parseSymbolDeclaration(TokenPattern<?> pattern, ISymbolContext ctx) {
        String memberName = pattern.find("SYMBOL_NAME").flatten(false);
        SymbolVisibility memberVisibility = TridentProductions.parseClassMemberVisibility(pattern.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL, this);
        final TokenPattern<?> entryFinal = pattern;


        VariableInstruction.SymbolDeclaration response = new VariableInstruction.SymbolDeclaration(memberName);
        response.setName(memberName);
        response.setVisibility(memberVisibility);
        response.setConstraintSupplier(initialValue -> (TypeConstraints) entryFinal.find("TYPE_CONSTRAINTS").evaluate(ctx, initialValue));
        response.setSupplier(() -> {
            Object initialValue = null;
            boolean initialized = false;
            if(pattern.find("SYMBOL_INITIALIZATION") != null) {
                DataStructureLiteralSet.setNextFunctionName(memberName);
                initialValue = pattern.find("SYMBOL_INITIALIZATION.INITIAL_VALUE").evaluate(ctx);
                DataStructureLiteralSet.setNextFunctionName(null);
                initialized = true;
            }
            Symbol sym = new Symbol(memberName, memberVisibility);
            sym.setTypeConstraints(response.getConstraint(initialValue));
            sym.setFinal(response.hasModifier(TridentTempFindABetterHome.SymbolModifier.FINAL));
            if(initialized) sym.safeSetValue(initialValue, entryFinal, ctx);
            return sym;
        });

        response.populate((TokenList) pattern.find("SYMBOL_MODIFIER_LIST"), ctx);

        return response;
    }
}
