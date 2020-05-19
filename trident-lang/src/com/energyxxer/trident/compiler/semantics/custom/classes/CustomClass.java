package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;
import com.energyxxer.util.logger.Debug;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction.parseSymbolDeclaration;

public class CustomClass implements TypeHandler<CustomClass> {
    private boolean complete = false;

    private final String name;
    private final HashMap<String, Symbol> staticMembers = new HashMap<>();
    private final LinkedHashMap<String, InstanceMemberSupplier> instanceMemberSuppliers = new LinkedHashMap<>();
    private ArrayList<CustomClass> superClasses;
    private Set<CustomClass> inheritanceTree = null;
    final LinkedHashMap<TypeHandler, TridentUserMethod> explicitCasts = new LinkedHashMap<>();
    final LinkedHashMap<TypeHandler, TridentUserMethod> implicitCasts = new LinkedHashMap<>();

    private Function<CustomClassObject, TridentMethod> constructorSupplier = null;
    private Symbol.SymbolVisibility constructorVisibility = Symbol.SymbolVisibility.LOCAL;

    private TridentFile definitionFile;
    private ISymbolContext definitionContext;

    private ISymbolContext innerContext;
    private String typeIdentifier;

    private ArrayList<TokenPattern<?>> forwardEntries = new ArrayList<>();

    private CustomClass(String name) {
        this.name = name;
    }

    //Constructor exclusively for native classes:
    public CustomClass(String name, String location, ISymbolContext ctx) {
        this.name = name;
        this.definitionContext = ctx;
        this.definitionFile = ctx.getStaticParentFile();
        this.innerContext = new SymbolContext(ctx);

        this.typeIdentifier = location + "@" + name;

        this.complete = true;
    }

    public static void defineClass(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.GLOBAL);
        String className = pattern.find("CLASS_NAME").flatten(false);

        TokenList bodyEntryList = (TokenList) pattern.find("CLASS_DECLARATION_BODY.CLASS_BODY_ENTRIES");
        boolean isCompleteDefinition = bodyEntryList != null;

        CustomClass classObject = ctx.getStaticParentFile().getClassForName(className);
        if(classObject != null) {
            if(classObject.isComplete()) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot modify definition of class '" + classObject.getTypeIdentifier() + "': Class is already complete", pattern.find("CLASS_NAME"), ctx);
            }
        } else {
            classObject = new CustomClass(className);
            classObject.definitionFile = ctx.getStaticParentFile();
            classObject.definitionContext = ctx;
            classObject.innerContext = new SymbolContext(ctx);
            classObject.typeIdentifier = classObject.definitionFile.getResourceLocation() + "@" + classObject.name;


            ctx.getStaticParentFile().registerInnerClass(classObject, pattern, ctx);
            Symbol sym = new Symbol(className, visibility, classObject);
            sym.setFinalAndLock();
            ctx.putInContextForVisibility(visibility, sym);
        }
        CustomClass finalClassObject = classObject;


        ArrayList<CustomClass> oldSuperClasses = classObject.superClasses;
        classObject.superClasses = new ArrayList<>();
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
        }

        if(isCompleteDefinition) {
            classObject.complete = true;
            for(TokenPattern<?> entry : bodyEntryList.getContents()) {
                entry = ((TokenStructure)entry).getContents();
                switch(entry.getName()) {
                    case "CLASS_MEMBER": {
                        VariableInstruction.SymbolDeclaration decl = parseSymbolDeclaration(entry, classObject.definitionContext);
                        decl.preparseConstraints();

                        if(decl.hasModifier(Symbol.SymbolModifier.STATIC)) {
                            classObject.putStaticMember(decl.getName(), decl.getSupplier().get());
                        } else {
                            classObject.putInstanceMember(decl.getName(), new InstanceMemberSupplier() {
                                @Override
                                public String getName() {
                                    return decl.getName();
                                }

                                @Override
                                public Symbol constructSymbol(CustomClassObject thiz) {
                                    return decl.getSupplier().get();
                                }

                                @Override
                                public boolean isFunction() {
                                    return false;
                                }

                                @Override
                                public Symbol.SymbolVisibility getVisibility() {
                                    return decl.getVisibility();
                                }
                            });
                        }
                        break;
                    }
                    case "CLASS_FUNCTION": {
                        String functionName = entry.find("SYMBOL_NAME").flatten(false);
                        boolean isConstructor = "new".equals(functionName);
                        VariableInstruction.SymbolModifierMap modifiers = VariableInstruction.SymbolModifierMap.createFromList(((TokenList) entry.find("SYMBOL_MODIFIER_LIST")), ctx);

                        if(isConstructor && modifiers.hasModifier(Symbol.SymbolModifier.STATIC)) {
                            throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "'static' modifier not allowed here", entry.find("SYMBOL_MODIFIER_LIST.LITERAL_STATIC"), classObject.definitionContext);
                        }

                        Symbol.SymbolVisibility memberVisibility = CommonParsers.parseVisibility(entry.find("SYMBOL_VISIBILITY"), classObject.definitionContext, Symbol.SymbolVisibility.LOCAL);
                        final TokenPattern<?> entryFinal = entry;

                        ArrayList<TridentMethodBranch> branches = new ArrayList<>();

                        TokenPattern<?> choice = ((TokenStructure)entry.find("CLASS_FUNCTION_SPLIT")).getContents();
                        switch(choice.getName()) {
                            case "DYNAMIC_FUNCTION": {
                                branches.add(TridentMethodBranch.parseDynamicFunction(choice, ctx));
                                break;
                            }
                            case "OVERLOADED_FUNCTION": {
                                TokenList implementations = (TokenList) choice.find("OVERLOADED_FUNCTION_IMPLEMENTATIONS");
                                for(TokenPattern<?> branch : implementations.getContents()) {
                                    branches.add(TridentMethodBranch.parseDynamicFunction(branch, ctx));
                                }
                                break;
                            }
                            default:
                                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + choice.getName() + "'", choice, ctx);
                        }

                        if(!isConstructor) {
                            Function<CustomClassObject, Symbol> memberSupplier = thiz -> {
                                ISymbolContext innerFrame = new SymbolContext(finalClassObject.innerContext);
                                if(thiz != null) {
                                    for(Symbol instanceSym : thiz.instanceMembers.values()) {
                                        innerFrame.put(instanceSym);
                                    }
                                }

                                TridentUserMethod method = new TridentUserMethod(functionName, branches, innerFrame, thiz);
                                Symbol sym = new Symbol(functionName, memberVisibility);
                                sym.setTypeConstraints(new TypeConstraints(TridentTypeManager.getHandlerForHandledClass(TridentMethod.class), false));
                                sym.setFinal(modifiers.hasModifier(Symbol.SymbolModifier.FINAL));
                                sym.safeSetValue(method, entryFinal, ctx);
                                return sym;
                            };
                            if(modifiers.hasModifier(Symbol.SymbolModifier.STATIC)) {
                                classObject.putStaticMember(functionName, memberSupplier.apply(null));
                            } else {
                                classObject.putInstanceMember(functionName, new InstanceMemberSupplier() {
                                    @Override
                                    public String getName() {
                                        return functionName;
                                    }

                                    @Override
                                    public Symbol constructSymbol(CustomClassObject thiz) {
                                        return memberSupplier.apply(thiz);
                                    }

                                    @Override
                                    public boolean isFunction() {
                                        return true;
                                    }

                                    @Override
                                    public Symbol.SymbolVisibility getVisibility() {
                                        return memberVisibility;
                                    }
                                });
                            }
                        } else {
                            classObject.setConstructor(memberVisibility, thiz -> new TridentUserMethod(functionName, branches, finalClassObject.innerContext, thiz));
                        }
                        break;
                    }
                    case "CLASS_FORWARD": {
                        classObject.forwardEntries.add(entry);
                        break;
                    }
                    case "CLASS_OVERRIDE": {
                        boolean implicit = "implicit".equals(entry.find("CLASS_TRANSFORM_TYPE").flatten(false));
                        TypeHandler toType = InterpolationManager.parseType(entry.find("INTERPOLATION_TYPE"), classObject.getInnerContext());
                        TridentMethodBranch branch = TridentMethodBranch.parseDynamicFunction(entry.find("DYNAMIC_FUNCTION"), classObject.getInnerContext());
                        TridentUserMethod function = new TridentUserMethod(toType.getTypeIdentifier(), Collections.singletonList(branch), classObject.getInnerContext(), null);
                        ((TridentUserMethodBranch) branch).setReturnConstraints(new TypeConstraints(toType, false));
                        if(implicit) ((TridentUserMethodBranch) branch).setShouldCoerce(false);
                        LinkedHashMap<TypeHandler, TridentUserMethod> castMap = implicit ? classObject.implicitCasts : classObject.explicitCasts;
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
    }

    public void putStaticFunction(TridentUserMethod value) {
        Symbol sym = new Symbol(name, Symbol.SymbolVisibility.PUBLIC, value);
        sym.setValue(value);
        sym.setFinalAndLock();
        putStaticMember(value.getFunctionName(), sym);
    }

    public void putStaticFinalMember(String name, Object value) {
        Symbol sym = new Symbol(name, Symbol.SymbolVisibility.PUBLIC, value);
        sym.setValue(value);
        sym.setFinalAndLock();
        putStaticMember(name, sym);
    }

    public void putStaticMember(String name, Symbol sym) {
        staticMembers.put(name, sym);
        innerContext.put(sym);
    }

    public void putInstanceMember(String name, InstanceMemberSupplier symSupplier) {
        instanceMemberSuppliers.put(name, symSupplier);
    }

    public void setConstructor(Symbol.SymbolVisibility visibility, Function<CustomClassObject, TridentMethod> supplier) {
        constructorVisibility = visibility;
        constructorSupplier = supplier;
    }

    @Override
    public Object getMember(CustomClass object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        for(CustomClass cls : getInheritanceTree()) {
            cls.assertComplete(pattern, ctx);
            if(cls.staticMembers.containsKey(member)) {
                Symbol sym = cls.staticMembers.get(member);

                if(cls.hasAccess(ctx, sym.getVisibility())) {
                    return keepSymbol ? sym : sym.getValue();
                } else {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "'" + sym.getName() + "' has " + sym.getVisibility().toString().toLowerCase() + " access in " + cls.getClassTypeIdentifier(), pattern, ctx);
                }
            }
        }
        return TridentTypeManager.getTypeHandlerTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
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
    public TypeHandler<?> getSuperType() {
        return null;
    }

    @Override
    public TridentMethod getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        assertComplete(pattern, ctx);
        if(ctx == null || hasAccess(ctx, constructorVisibility)) {
            return (params, patterns, pattern2, ctx2) -> {
                CustomClassObject created = new CustomClassObject(this);

                for(CustomClass cls : getInheritanceTree()) {
                    for(InstanceMemberSupplier symbolSupplier : cls.instanceMemberSuppliers.values()) {
                        created.putMemberIfAbsent(symbolSupplier.constructSymbol(created));
                    }
                }

                if(constructorSupplier != null) {

                    ISymbolContext innerFrame = new SymbolContext(this.innerContext);
                    for(Symbol instanceSym : created.instanceMembers.values()) {
                        innerFrame.put(instanceSym);
                    }

                    constructorSupplier.apply(created).safeCall(params, patterns, pattern2, ctx2);
                    for(Symbol field : created.instanceMembers.values()) {
                        if(field.isFinal() && field.maySet()) {
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Final symbol '" + field.getName() + "' was not initialized in constructor.", pattern, ctx2);
                        }
                    }
                }

                for(TokenPattern<?> entry : forwardEntries) {
                    String forwardTargetName = entry.find("FORWARD_TARGET_NAME").flatten(false);
                    Symbol.SymbolVisibility forwardVisibility = CommonParsers.parseVisibility(entry.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.LOCAL);

                    Symbol targetSym = created.getSymbol(forwardTargetName);
                    if(targetSym == null) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Unknown symbol '" + forwardTargetName + "'", entry, ctx2);
                    }
                    if(!targetSym.isFinal()) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + forwardTargetName + "' is not final.", entry, ctx2);
                    }
                    if(targetSym.getValue() == null) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + forwardTargetName + "' is not initialized.", entry, ctx2);
                    }
                    CustomClass forwardTargetType;
                    if(targetSym.getTypeConstraints() != null && targetSym.getTypeConstraints().getHandler() instanceof CustomClass) {
                        forwardTargetType = ((CustomClass) targetSym.getTypeConstraints().getHandler());
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "The type of symbol '" + forwardTargetName + "' is not constrained to a class type", entry, ctx2);
                    }



                    TokenList memberList = ((TokenList) entry.find("FORWARD_MEMBER_NAMES"));
                    for(TokenPattern<?> rawMember : memberList.searchByName("FORWARD_MEMBER_NAME")) {
                        String rawMemberName = rawMember.flatten(false);
                        if("*".equals(rawMemberName)) {
                            //wildcard, forward all functions into the instance
                            for(InstanceMemberSupplier forwardTypeMemberSupplier : forwardTargetType.instanceMemberSuppliers.values()) {
                                if(forwardTypeMemberSupplier.isFunction() && forwardTargetType.hasAccess(innerContext, forwardTypeMemberSupplier.getVisibility())) {
                                    Debug.log("forwarded: " + forwardTypeMemberSupplier.getName());
                                    Symbol forwardedSym = new Symbol(forwardTypeMemberSupplier.getName(), forwardVisibility);
                                    Symbol targetFunctionSymbol = ((CustomClassObject) targetSym.getValue()).instanceMembers.get(forwardTypeMemberSupplier.getName());
                                    forwardedSym.setValue((TridentMethod) (params1, patterns1, pattern1, ctx1) -> ((TridentMethod) targetFunctionSymbol.getValue()).safeCall(params1, patterns1, pattern1, ctx1));
                                    forwardedSym.setFinalAndLock();
                                    created.putMemberIfAbsent(((CustomClassObject) targetSym.getValue()).instanceMembers.get(forwardTypeMemberSupplier.getName()));
                                }
                            }
                        } else {
                            InstanceMemberSupplier forwardTypeMemberSupplier = forwardTargetType.instanceMemberSuppliers.get(rawMemberName);
                            if(forwardTypeMemberSupplier == null) {
                                throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + rawMemberName + "' does not exist in the forward target", rawMember, ctx2);
                            }
                            if(forwardTypeMemberSupplier.isFunction()) {
                                if(forwardTargetType.hasAccess(innerContext, forwardTypeMemberSupplier.getVisibility())) {
                                    Debug.log("forwarded: " + forwardTypeMemberSupplier.getName());
                                    Symbol forwardedSym = new Symbol(forwardTypeMemberSupplier.getName(), forwardVisibility);
                                    Symbol targetFunctionSymbol = ((CustomClassObject) targetSym.getValue()).instanceMembers.get(forwardTypeMemberSupplier.getName());
                                    forwardedSym.setValue((TridentMethod) (params1, patterns1, pattern1, ctx1) -> ((TridentMethod) targetFunctionSymbol.getValue()).safeCall(params1, patterns1, pattern1, ctx1));
                                    forwardedSym.setFinalAndLock();
                                    created.putMemberIfAbsent(((CustomClassObject) targetSym.getValue()).instanceMembers.get(forwardTypeMemberSupplier.getName()));
                                } else {
                                    throw new TridentException(TridentException.Source.TYPE_ERROR, "'" + rawMemberName + "' has " + forwardTypeMemberSupplier.getVisibility().toString().toLowerCase() + " access in " + forwardTargetType.getClassTypeIdentifier(), rawMember, ctx2);
                                }
                            } else {
                                throw new TridentException(TridentException.Source.TYPE_ERROR, "The forward member must be a function", rawMember, ctx2);
                            }
                        }
                    }
                }
                return created;
            };
        } else {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Constructor has " + constructorVisibility.toString().toLowerCase() + " access in " + getClassTypeIdentifier(), pattern, ctx);
        }
    }

    public ISymbolContext getDeclaringContext() {
        return definitionContext;
    }

    public TridentFile getDeclaringFile() {
        return definitionFile;
    }

    public ISymbolContext getInnerContext() {
        return innerContext;
    }

    public boolean hasAccess(ISymbolContext ctx, Symbol.SymbolVisibility visibility) {
        return visibility == Symbol.SymbolVisibility.PUBLIC ||
                (visibility == Symbol.SymbolVisibility.LOCAL && getDeclaringFile().getDeclaringFSFile().equals(ctx.getDeclaringFSFile())) ||
                (visibility == Symbol.SymbolVisibility.PRIVATE && ctx.isAncestor(this.innerContext));
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

    private interface InstanceMemberSupplier {
        String getName();
        Symbol constructSymbol(CustomClassObject thiz);

        boolean isFunction();
        Symbol.SymbolVisibility getVisibility();
    }
}
