package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;
import com.energyxxer.util.logger.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public class CustomClass implements TypeHandler<CustomClass> {

    private final String name;
    private final HashMap<String, Symbol> staticMembers = new HashMap<>();
    private final ArrayList<Function<CustomClassObject, Symbol>> instanceMemberSuppliers = new ArrayList<>();
    private final CustomClass superClass = null;

    private Function<CustomClassObject, TridentMethod> constructorSupplier = null;
    private Symbol.SymbolVisibility constructorVisibility = Symbol.SymbolVisibility.LOCAL;

    private TridentFile definitionFile;
    private ISymbolContext definitionContext;

    private ISymbolContext innerContext;

    public CustomClass(String name) {
        this.name = name;
    }

    public static void defineClass(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.GLOBAL);
        String className = pattern.find("CLASS_NAME").flatten(false);

        CustomClass classObject = new CustomClass(className);
        classObject.definitionFile = ctx.getStaticParentFile();
        classObject.definitionContext = ctx;

        ctx.putInContextForVisibility(visibility, new Symbol(className, visibility, classObject));

        classObject.innerContext = new SymbolContext(ctx);

        TokenList bodyEntryList = (TokenList) pattern.find("CLASS_DECLARATION_BODY.CLASS_BODY_ENTRIES");

        if(bodyEntryList != null) {
            for(TokenPattern<?> entry : bodyEntryList.getContents()) {
                entry = ((TokenStructure)entry).getContents();
                switch(entry.getName()) {
                    case "CLASS_MEMBER": {
                        boolean isStatic = entry.find("LITERAL_STATIC") != null;

                        String memberName = entry.find("MEMBER_NAME").flatten(false);
                        Symbol.SymbolVisibility memberVisibility = CommonParsers.parseVisibility(entry.find("SYMBOL_VISIBILITY"), classObject.definitionContext, Symbol.SymbolVisibility.LOCAL);
                        final TokenPattern<?> entryFinal = entry;

                        Function<CustomClassObject, Symbol> memberSupplier = thiz -> {
                            Object initialValue = InterpolationManager.parse(entryFinal.find("FIELD_INITIALIZATION.INTERPOLATION_VALUE"), classObject.definitionContext);
                            Symbol sym = new Symbol(memberName, memberVisibility);
                            sym.setTypeConstraints(TypeConstraints.parseConstraintsInfer(entryFinal.find("TYPE_CONSTRAINTS"), classObject.definitionContext, initialValue));
                            sym.safeSetValue(initialValue, entryFinal, classObject.definitionContext);
                            return sym;
                        };

                        if(isStatic) {
                            Symbol staticSym = memberSupplier.apply(null);
                            classObject.staticMembers.put(memberName, staticSym);
                            classObject.innerContext.put(staticSym);
                        } else {
                            classObject.instanceMemberSuppliers.add(memberSupplier);
                        }
                        break;
                    }
                    case "CLASS_FUNCTION": {
                        String functionName = entry.find("MEMBER_NAME").flatten(false);
                        boolean isConstructor = "new".equals(functionName);
                        boolean isStatic = entry.find("LITERAL_STATIC") != null;

                        if(isConstructor && isStatic) {
                            throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "'static' modifier not allowed here", entry.find("LITERAL_STATIC"), classObject.definitionContext);
                        }

                        Symbol.SymbolVisibility memberVisibility = CommonParsers.parseVisibility(entry.find("SYMBOL_VISIBILITY"), classObject.definitionContext, Symbol.SymbolVisibility.LOCAL);
                        final TokenPattern<?> entryFinal = entry;

                        ArrayList<FormalParameter> formalParams = new ArrayList<>();
                        TypeConstraints returnConstraints = TypeConstraints.parseConstraints(entry.find("TYPE_CONSTRAINTS"), ctx);
                        TokenList paramNames = (TokenList) entry.find("FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST");
                        if(paramNames != null) {
                            for(TokenPattern<?> param : paramNames.searchByName("FORMAL_PARAMETER")) {
                                formalParams.add(new FormalParameter(param.find("FORMAL_PARAMETER_NAME").flatten(false), TypeConstraints.parseConstraints(param.find("TYPE_CONSTRAINTS"), classObject.definitionContext)));
                            }
                        }


                        TokenPattern<?> innerFunctionPattern = entry.find("ANONYMOUS_INNER_FUNCTION");

                        if(!isConstructor) {
                            Function<CustomClassObject, Symbol> memberSupplier = thiz -> {
                                ISymbolContext innerFrame = new SymbolContext(classObject.innerContext);
                                if(thiz != null) {
                                    for(Symbol instanceSym : thiz.instanceMembers.values()) {
                                        innerFrame.put(instanceSym);
                                    }
                                }
                                TridentUserMethod method = new TridentUserMethod(innerFunctionPattern, innerFrame, formalParams, thiz, functionName);
                                method.setReturnConstraints(returnConstraints);
                                Symbol sym = new Symbol(functionName, memberVisibility);
                                sym.setTypeConstraints(new TypeConstraints(TridentTypeManager.getHandlerForHandledClass(TridentMethod.class), false));
                                sym.setFinal(true);
                                sym.safeSetValue(method, entryFinal, ctx);
                                return sym;
                            };
                            if(isStatic) {
                                Symbol staticSym = memberSupplier.apply(null);
                                classObject.staticMembers.put(functionName, staticSym);
                                classObject.innerContext.put(staticSym);
                            } else {
                                classObject.instanceMemberSuppliers.add(memberSupplier);
                            }
                        } else {
                            classObject.constructorSupplier = thiz -> new TridentUserMethod(innerFunctionPattern, classObject.innerContext, formalParams, thiz, functionName);
                            classObject.constructorVisibility = memberVisibility;
                        }
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

        Debug.log(classObject.staticMembers);
    }

    @Override
    public Object getMember(CustomClass object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(staticMembers.containsKey(member)) {
            Symbol sym = staticMembers.get(member);

            if(hasAccess(ctx, sym.getVisibility())) {
                return keepSymbol ? sym : sym.getValue();
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "'" + sym.getName() + "' has " + sym.getVisibility().toString().toLowerCase() + " access in " + getClassTypeIdentifier(), pattern, ctx);
            }
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(CustomClass object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(CustomClass object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        return name;
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
        //TODO: Inheritance
        return obj instanceof CustomClassObject && ((CustomClassObject) obj).getType() == this;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    String getClassTypeIdentifier() {
        return definitionFile.getResourceLocation() + "@" + name;
    }

    @Override
    public String getTypeIdentifier() {
        return getClassTypeIdentifier();
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return superClass;
    }

    @Override
    public TridentMethod getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(hasAccess(ctx, constructorVisibility)) {
            return (params, patterns, pattern2, ctx2) -> {
                CustomClassObject created = new CustomClassObject(this);
                for(Function<CustomClassObject, Symbol> symbolSupplier : instanceMemberSuppliers) {
                    created.putMember(symbolSupplier.apply(created));
                }

                if(constructorSupplier != null) {

                    ISymbolContext innerFrame = new SymbolContext(this.innerContext);
                    for(Symbol instanceSym : created.instanceMembers.values()) {
                        innerFrame.put(instanceSym);
                    }

                    constructorSupplier.apply(created).safeCall(params, patterns, pattern2, ctx2);
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

    public boolean hasAccess(ISymbolContext ctx, Symbol.SymbolVisibility visibility) {
        return visibility == Symbol.SymbolVisibility.PUBLIC ||
                (visibility == Symbol.SymbolVisibility.LOCAL && getDeclaringFile().getDeclaringFSFile().equals(ctx.getDeclaringFSFile())) ||
                (visibility == Symbol.SymbolVisibility.PRIVATE && ctx.isAncestor(this.innerContext));
    }

    @Override
    public boolean isStaticHandler() {
        return true;
    }
}
