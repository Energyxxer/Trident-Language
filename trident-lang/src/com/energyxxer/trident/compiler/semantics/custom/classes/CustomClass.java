package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.ActualParameterList;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ClassMethodSymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction.parseSymbolDeclaration;
import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

public class CustomClass implements TypeHandler<CustomClass>, ParameterizedMemberHolder {
    private static final CustomClass BASE_CLASS = new CustomClass();


    enum MemberParentMode {
        CREATE, OVERRIDE, FORCE, INHERIT;
    }

    private boolean complete = false;

    private final String name;
    private final HashMap<String, Symbol> staticMembers = new HashMap<>();
    private final LinkedHashMap<String, InstanceMemberSupplier> instanceMemberSuppliers = new LinkedHashMap<>();
    private ArrayList<CustomClass> superClasses;
    private Set<CustomClass> inheritanceTree = null;
    final LinkedHashMap<TypeHandler, TridentUserFunction> explicitCasts = new LinkedHashMap<>();
    final LinkedHashMap<TypeHandler, TridentUserFunction> implicitCasts = new LinkedHashMap<>();

    private final ClassMethodTable staticMethods = new ClassMethodTable(this);
    final ClassMethodTable instanceMethods = new ClassMethodTable(this);
    private final ClassIndexerFamily indexers = new ClassIndexerFamily();

    private ClassMethodFamily constructorFamily = null;

    private TridentFile definitionFile;
    private ISymbolContext definitionContext;

    private ClassMethodSymbolContext innerStaticContext;
    private String typeIdentifier;

    //Constructor exclusively for base class
    private CustomClass() {
        this.name = "BASE CLASS";
        this.definitionContext = null;
        this.definitionFile = null;
        this.innerStaticContext = null;
        this.typeIdentifier = "<base class>";
        this.complete = true;

        try {
            ClassMethod baseToStringMethod = new ClassMethod(this, null, nativeMethodsToFunction(null, "toString", CustomClass.class.getMethod("defaultToString", CustomClassObject.class)));
            baseToStringMethod.setVisibility(Symbol.SymbolVisibility.PUBLIC);
            this.instanceMethods.put(baseToStringMethod, MemberParentMode.FORCE, null, null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static @NativeMethodWrapper.TridentNotNullReturn String defaultToString(@NativeMethodWrapper.TridentThisArg CustomClassObject obj) {
        return obj.toString();
    }

    private CustomClass(String name) {
        this.name = name;
    }

    //Constructor exclusively for native classes:
    public CustomClass(String name, String location, ISymbolContext ctx) {
        this.name = name;
        this.definitionContext = ctx;
        this.definitionFile = ctx.getStaticParentFile();
        this.innerStaticContext = new ClassMethodSymbolContext(ctx, null);

        this.typeIdentifier = location + "@" + name;

        this.complete = true;
    }

    public static void defineClass(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.GLOBAL);
        String className = pattern.find("CLASS_NAME").flatten(false);

        TokenList bodyEntryList = (TokenList) pattern.find("CLASS_DECLARATION_BODY.CLASS_BODY_ENTRIES");
        boolean isCompleteDefinition = pattern.find("CLASS_DECLARATION_BODY") != null;

        CustomClass classObject = ctx.getStaticParentFile().getClassForName(className);
        if(classObject != null) {
            if(classObject.isComplete()) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot modify definition of class '" + classObject.getTypeIdentifier() + "': Class is already complete", pattern.find("CLASS_NAME"), ctx);
            }
        } else {
            classObject = new CustomClass(className);
            classObject.definitionFile = ctx.getStaticParentFile();
            classObject.definitionContext = ctx;
            classObject.innerStaticContext = new ClassMethodSymbolContext(ctx, null);
            classObject.typeIdentifier = classObject.definitionFile.getResourceLocation() + "@" + classObject.name;


            ctx.getStaticParentFile().registerInnerClass(classObject, pattern, ctx);
            Symbol sym = new Symbol(className, visibility, classObject);
            sym.setFinalAndLock();
            ctx.putInContextForVisibility(visibility, sym);
        }
        CustomClass finalClassObject = classObject;


        ArrayList<CustomClass> oldSuperClasses = classObject.superClasses;
        classObject.superClasses = new ArrayList<>();
        classObject.superClasses.add(BASE_CLASS);
        if(pattern.find("CLASS_INHERITS") != null) {
            TokenList inheritsList = ((TokenList) pattern.find("CLASS_INHERITS.SUPERCLASS_LIST"));
            for(TokenPattern<?> rawParent : inheritsList.searchByName("INTERPOLATION_TYPE")) {
                TypeHandler parentType = InterpolationManager.parseType(rawParent, ctx);
                if(parentType instanceof CustomClass) {
                    if(classObject.superClasses.contains(parentType)) {
                        throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "Duplicated superclass: " + ((CustomClass) parentType).typeIdentifier + "", rawParent, ctx);
                    } else {
                        classObject.superClasses.add((CustomClass) parentType);
                    }
                } else {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "'" + parentType.getTypeIdentifier() + "' is not a class type", rawParent, ctx);
                }
            }
        }
        if(isCompleteDefinition) {
            if(oldSuperClasses != null && !classObject.superClasses.containsAll(oldSuperClasses)) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Incomplete definition promised to extend " + oldSuperClasses + "; not all inherited in the complete definition.", pattern.tryFind("CLASS_INHERITS"), ctx);
            }

            classObject.complete = true;
            for(CustomClass superClass : classObject.getInheritanceTree()) {
                if(superClass == classObject) continue;
                classObject.instanceMethods.putAll(superClass.instanceMethods, pattern.find("CLASS_NAME"), ctx);
                classObject.indexers.putAll(superClass.indexers, pattern.find("CLASS_NAME"), ctx);
            }
            if(bodyEntryList != null) {
                for(TokenPattern<?> entry : bodyEntryList.getContents()) {
                    entry = ((TokenStructure)entry).getContents();
                    switch(entry.getName()) {
                        case "CLASS_MEMBER": {
                            VariableInstruction.SymbolDeclaration decl = parseSymbolDeclaration(entry, classObject.definitionContext);
                            decl.preParseConstraints();
                            MemberParentMode mode = MemberParentMode.CREATE;
                            if(entry.find("MEMBER_PARENT_MODE") != null) {
                                mode = MemberParentMode.valueOf(entry.find("MEMBER_PARENT_MODE").flatten(false).toUpperCase());
                            }

                            if(decl.hasModifier(Symbol.SymbolModifier.STATIC)) {
                                if(mode != MemberParentMode.CREATE) {
                                    throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot " + mode.toString().toLowerCase() + " a static member", entry.find("MEMBER_PARENT_MODE"), ctx);
                                }
                                classObject.putStaticMember(decl.getName(), decl.getSupplier().get());
                            } else {
                                if(mode == MemberParentMode.CREATE) {
                                    InstanceMemberSupplier alreadyDefinedSupplier = classObject.getInstanceMemberSupplier(decl.getName());
                                    if(alreadyDefinedSupplier != null && alreadyDefinedSupplier.getDefiningClass() == classObject) {
                                        throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "Duplicate member '" + decl.getName() + "': it's already defined in the same class", entry, ctx);
                                    } if(alreadyDefinedSupplier != null) {
                                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Member '" + decl.getName() + "' is already defined in inherited class " + alreadyDefinedSupplier.getDefiningClass().typeIdentifier + ". Use the 'override' keyword to change its default value", entry, ctx);
                                    }
                                } else if(mode == MemberParentMode.OVERRIDE) {
                                    InstanceMemberSupplier alreadyDefinedSupplier = classObject.getInstanceMemberSupplier(decl.getName());
                                    if(alreadyDefinedSupplier == null) {
                                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override member '" + decl.getName() + "': not found in any of the inherited classes", entry, ctx);
                                    } else if(alreadyDefinedSupplier.getDefiningClass() == classObject) {
                                        throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "Cannot override member '" + decl.getName() + "': it's already defined in the same class", entry, ctx);
                                    }
                                    if(((InstanceFieldSupplier) alreadyDefinedSupplier).getDecl().hasModifier(Symbol.SymbolModifier.FINAL)) {
                                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override field '" + decl.getName() + "': it's defined as final in " + alreadyDefinedSupplier.getDefiningClass().typeIdentifier, entry, ctx);
                                    } else if(decl.hasModifier(Symbol.SymbolModifier.FINAL)) {
                                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override field '" + decl.getName() + "' with a final member", entry, ctx);
                                    }

                                    TypeConstraints thisConstraints = decl.getConstraint(null);
                                    TypeConstraints otherConstraints = ((InstanceFieldSupplier) alreadyDefinedSupplier).getDecl().getConstraint(null);
                                    if (!TypeConstraints.constraintsEqual(thisConstraints, otherConstraints)) {
                                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override field '" + decl.getName() + "': Mismatch of type constraints. Defined in superclass " + alreadyDefinedSupplier.getDefiningClass().typeIdentifier + ": " + otherConstraints + "; Found: " + thisConstraints, entry, ctx);
                                    }
                                }

                                classObject.putInstanceMember(decl.getName(), new InstanceFieldSupplier(decl) {
                                    @Override
                                    public String getName() {
                                        return decl.getName();
                                    }

                                    @Override
                                    public Symbol constructSymbol(CustomClassObject thiz) {
                                        return decl.getSupplier().get();
                                    }

                                    @Override
                                    public Symbol.SymbolVisibility getVisibility() {
                                        return decl.getVisibility();
                                    }

                                    @Override
                                    public CustomClass getDefiningClass() {
                                        return finalClassObject;
                                    }
                                });
                            }
                            break;
                        }
                        case "CLASS_FUNCTION": {
                            String functionName = entry.find("SYMBOL_NAME").flatten(false);
                            boolean isConstructor = "new".equals(functionName);
                            VariableInstruction.SymbolModifierMap modifiers = VariableInstruction.SymbolModifierMap.createFromList(((TokenList) entry.find("SYMBOL_MODIFIER_LIST")), ctx);
                            MemberParentMode mode = MemberParentMode.CREATE;
                            if(entry.find("MEMBER_PARENT_MODE") != null) {
                                mode = MemberParentMode.valueOf(entry.find("MEMBER_PARENT_MODE").flatten(false).toUpperCase());
                            }

                            if(isConstructor && modifiers.hasModifier(Symbol.SymbolModifier.STATIC)) {
                                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "'static' modifier not allowed here", entry.find("SYMBOL_MODIFIER_LIST.LITERAL_STATIC"), classObject.definitionContext);
                            }

                            Symbol.SymbolVisibility memberVisibility = CommonParsers.parseVisibility(entry.find("SYMBOL_VISIBILITY"), classObject.definitionContext, Symbol.SymbolVisibility.LOCAL);

                            TridentFunctionBranch branch = TridentFunctionBranch.parseDynamicFunction(entry.find("DYNAMIC_FUNCTION"), ctx);

                            if(!isConstructor) {
                                ClassMethod method = new ClassMethod(
                                        finalClassObject,
                                        entry,
                                        new TridentUserFunction(
                                                functionName,
                                                branch,
                                                finalClassObject.prepareFunctionContext(),
                                                null
                                        )
                                ).setVisibility(memberVisibility).setModifiers(modifiers);

                                if(modifiers.hasModifier(Symbol.SymbolModifier.STATIC)) {
                                    classObject.staticMethods.put(method, mode, entry, ctx);
                                    classObject.innerStaticContext.putClassFunction(classObject.staticMethods.getFamily(functionName));
                                } else {
                                    classObject.instanceMethods.put(method, mode, entry, ctx);
                                }
                            } else {
                                ClassMethod method = new ClassMethod(
                                        finalClassObject,
                                        entry,
                                        new TridentUserFunction(
                                                functionName,
                                                branch,
                                                finalClassObject.prepareFunctionContext(),
                                                null
                                        )
                                ).setVisibility(memberVisibility).setModifiers(modifiers);

                                if(classObject.constructorFamily == null) {
                                    classObject.constructorFamily = new ClassMethodFamily("new");
                                }
                                classObject.constructorFamily.putOverload(method, mode, entry, ctx);
                            }
                            break;
                        }
                        case "CLASS_INDEXER": {
                            Symbol.SymbolVisibility defaultVisibility = CommonParsers.parseVisibility(entry.find("SYMBOL_VISIBILITY"), classObject.definitionContext, Symbol.SymbolVisibility.LOCAL);
                            MemberParentMode mode = MemberParentMode.CREATE;
                            if(entry.find("MEMBER_PARENT_MODE") != null) {
                                mode = MemberParentMode.valueOf(entry.find("MEMBER_PARENT_MODE").flatten(false).toUpperCase());
                            }

                            FormalParameter indexParam = FormalParameter.create(entry.find("FORMAL_PARAMETER"), ctx);

                            TridentUserFunctionBranch getterBranch = new TridentUserFunctionBranch(Collections.singletonList(indexParam), entry.find("CLASS_GETTER.ANONYMOUS_INNER_FUNCTION"), TypeConstraints.parseConstraints(entry.find("CLASS_GETTER.TYPE_CONSTRAINTS"), ctx));
                            Symbol.SymbolVisibility getterVisibility = CommonParsers.parseVisibility(entry.find("CLASS_GETTER.SYMBOL_VISIBILITY"), classObject.definitionContext, defaultVisibility);
                            TridentUserFunction getter = new TridentUserFunction(
                                    "<indexer getter>",
                                    getterBranch,
                                    finalClassObject.prepareFunctionContext(),
                                    null
                            );

                            TridentUserFunction setter = null;
                            Symbol.SymbolVisibility setterVisibility = Symbol.SymbolVisibility.LOCAL;
                            if(entry.find("CLASS_SETTER") != null) {
                                setterVisibility = CommonParsers.parseVisibility(entry.find("CLASS_SETTER.SYMBOL_VISIBILITY"), classObject.definitionContext, defaultVisibility);
                                TridentUserFunctionBranch setterBranch = new TridentUserFunctionBranch(Arrays.asList(indexParam, FormalParameter.create(entry.find("CLASS_SETTER.FORMAL_PARAMETER"), ctx)), entry.find("CLASS_SETTER.ANONYMOUS_INNER_FUNCTION"), null);
                                setter = new TridentUserFunction(
                                        "<indexer setter>",
                                        setterBranch,
                                        finalClassObject.prepareFunctionContext(),
                                        null
                                );
                            }

                            if(getterVisibility.getVisibilityIndex() > defaultVisibility.getVisibilityIndex()) {
                                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Getter access privileges must not be more accessible than the indexer's. Indexer access: '" + defaultVisibility.toString().toLowerCase() + "', Getter access: " + getterVisibility.toString().toLowerCase(), entry.tryFind("CLASS_GETTER.SYMBOL_VISIBILITY"), ctx);
                            }
                            if(setter != null && setterVisibility.getVisibilityIndex() > defaultVisibility.getVisibilityIndex()) {
                                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Setter access privileges must not be more accessible than the indexer's. Indexer access: '" + defaultVisibility.toString().toLowerCase() + "', Setter access: " + setterVisibility.toString().toLowerCase(), entry.tryFind("CLASS_SETTER.SYMBOL_VISIBILITY"), ctx);
                            }

                            ClassIndexer indexer = new ClassIndexer(classObject, entry, indexParam, getter, setter);
                            indexer.setGetterVisibility(getterVisibility);
                            indexer.setSetterVisibility(setterVisibility);

                            classObject.indexers.put(indexer, mode, entry, ctx);
                            break;
                        }
                        case "CLASS_OVERRIDE": {
                            boolean implicit = "implicit".equals(entry.find("CLASS_TRANSFORM_TYPE").flatten(false));
                            TypeHandler toType = InterpolationManager.parseType(entry.find("INTERPOLATION_TYPE"), classObject.getInnerStaticContext());
                            TridentFunctionBranch branch = TridentFunctionBranch.parseDynamicFunction(entry.find("DYNAMIC_FUNCTION"), classObject.getInnerStaticContext());
                            TridentUserFunction function = new TridentUserFunction(toType.getTypeIdentifier(), branch, classObject.getInnerStaticContext(), null);
                            branch.setReturnConstraints(new TypeConstraints(toType, false));
                            if(implicit) branch.setShouldCoerce(false);
                            LinkedHashMap<TypeHandler, TridentUserFunction> castMap = implicit ? classObject.implicitCasts : classObject.explicitCasts;
                            castMap.put(toType, function);
                            break;
                        }
                        case "COMMENT": {
                            break;
                        }
                        default: {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + entry.getName() + "'", entry, ctx);
                        }
                    }
                }
            }

            classObject.instanceMethods.checkClashingInheritedMethodsResolved(pattern.find("CLASS_NAME"), ctx);
            classObject.indexers.checkClashingInheritedIndexersResolved(classObject, pattern.find("CLASS_NAME"), ctx);

            for(Symbol field : classObject.staticMembers.values()) {
                if(field.isFinal() && field.maySet()) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Final static symbol '" + field.getName() + "' was not initialized.", pattern, ctx);
                }
            }
        }

    }

    public void putStaticFunction(TridentUserFunction value) {
        ClassMethod method = new ClassMethod(
                this,
                null,
                new TridentUserFunction(
                        value.getFunctionName(),
                        value.getBranch(),
                        this.prepareFunctionContext(),
                        null
                )
        ).setVisibility(Symbol.SymbolVisibility.PUBLIC);

        this.staticMethods.put(method, MemberParentMode.FORCE, null, null);
        this.innerStaticContext.putMethod(this.staticMethods.getFamily(value.getFunctionName()));
    }

    public void putStaticFinalMember(String name, Object value) {
        Symbol sym = new Symbol(name, Symbol.SymbolVisibility.PUBLIC, value);
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

    public void setNoConstructor() {
        constructorFamily = new ClassMethodFamily("new");
        ClassMethod method = new ClassMethod("new", this).setVisibility(Symbol.SymbolVisibility.PRIVATE);
        method.setModifiers(new VariableInstruction.SymbolModifierMap().setModifier(Symbol.SymbolModifier.FINAL));

        constructorFamily.putOverload(
                method,
                MemberParentMode.FORCE,
                null,
                null
        );
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
                throw new TridentException(TridentException.Source.TYPE_ERROR, "'" + sym.getName() + "' has " + sym.getVisibility().toString().toLowerCase() + " access in " + object.getClassTypeIdentifier(), pattern, ctx);
            }
        }
        return TridentTypeManager.getTypeHandlerTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
    }

    @Override
    public Object getMemberForParameters(String memberName, TokenPattern<?> pattern, ActualParameterList params, ISymbolContext ctx, boolean keepSymbol) {
        Object foundClassMethod = staticMethods.find(memberName, params, pattern, ctx);
        if(foundClassMethod == null) {
            try {
                foundClassMethod = getMember(this, memberName, pattern, ctx, keepSymbol);
            } catch(MemberNotFoundException ignore) {
            }
        }
        if(foundClassMethod == null) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot resolve function or method '" + memberName + "' of " + TridentTypeManager.getTypeIdentifierForObject(this), pattern, ctx);
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
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Definition of class '" + getTypeIdentifier() + "' is not yet complete", pattern, ctx);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getIndexer(CustomClass object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return TridentTypeManager.getTypeHandlerTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object cast(CustomClass object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return TridentTypeManager.getTypeHandlerTypeHandler().cast(object, targetType, pattern, ctx);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object coerce(CustomClass object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return TridentTypeManager.getTypeHandlerTypeHandler().coerce(object, targetType, pattern, ctx);
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into) {
        return TridentTypeManager.getTypeHandlerTypeHandler().canCoerce(object, into);
    }

    @Override
    public boolean isSubType(TypeHandler<?> other) {
        return other instanceof CustomClass && ((CustomClass) other).getInheritanceTree().contains(this);
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
        return obj instanceof CustomClassObject && ((CustomClassObject) obj).getType().getInheritanceTree().contains(this);
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
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        assertComplete(pattern, ctx);
        return (params, patterns, pattern2, ctx2) -> {
            CustomClassObject created = new CustomClassObject(this);

            for(CustomClass cls : getInheritanceTree()) {
                for(InstanceMemberSupplier symbolSupplier : cls.instanceMemberSuppliers.values()) {
                    if(!created.containsMember(symbolSupplier.getName()))
                        created.putMemberIfAbsent(symbolSupplier.constructSymbol(created));
                }
            }

            if(constructorFamily != null) {
                ClassMethodFamily.ClassMethodSymbol pickedConstructor = constructorFamily.pickOverloadSymbol(new ActualParameterList(Arrays.asList(params), Arrays.asList(patterns), pattern2), pattern2, ctx, created);
                pickedConstructor.safeCall(params, patterns, pattern2, ctx2);
                for(Symbol field : created.instanceMembers.values()) {
                    if(field.isFinal() && field.maySet()) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Final symbol '" + field.getName() + "' was not initialized in constructor.", pattern, ctx2);
                    }
                }
            }

            return created;
        };
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

    public boolean hasAccess(ISymbolContext ctx, Symbol.SymbolVisibility visibility) {
        return visibility == Symbol.SymbolVisibility.PUBLIC ||
                (visibility == Symbol.SymbolVisibility.LOCAL && getDeclaringFile().getDeclaringFSFile().equals(ctx.getDeclaringFSFile())) ||
                (visibility == Symbol.SymbolVisibility.PRIVATE && ctx.isAncestor(this.innerStaticContext));
    }

    @Override
    public boolean isStaticHandler() {
        return true;
    }

    public Object forceInstantiate() {
        return null;
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

    private interface InstanceMemberSupplier {
        CustomClass getDefiningClass();
        String getName();
        Symbol constructSymbol(CustomClassObject thiz);

        Symbol.SymbolVisibility getVisibility();
    }

    private static abstract class InstanceFieldSupplier implements InstanceMemberSupplier {
        private final VariableInstruction.SymbolDeclaration decl;

        public InstanceFieldSupplier(VariableInstruction.SymbolDeclaration decl) {
            this.decl = decl;
        }

        public VariableInstruction.SymbolDeclaration getDecl() {
            return decl;
        }
    }

    //public TridentFunction createFunction(CustomClassObject thiz) {
    //    ISymbolContext innerFrame = definingClass.prepareFunctionContext(thiz);
    //    return new TridentUserFunction(functionName, branches, innerFrame, thiz);
    //}
}
