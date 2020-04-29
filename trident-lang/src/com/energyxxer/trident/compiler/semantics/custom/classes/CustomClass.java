package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import static com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction.parseSymbolDeclaration;

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
                        VariableInstruction.SymbolDeclaration decl = parseSymbolDeclaration(entry, classObject.definitionContext);

                        if(decl.hasModifier(Symbol.SymbolModifier.STATIC)) {
                            Symbol staticSym = decl.getSupplier().get();
                            classObject.staticMembers.put(decl.getName(), staticSym);
                            classObject.innerContext.put(staticSym);
                        } else {
                            classObject.instanceMemberSuppliers.add(thiz -> decl.getSupplier().get());
                        }
                        break;
                    }
                    case "CLASS_FUNCTION": {
                        String functionName = entry.find("SYMBOL_NAME").flatten(false);
                        boolean isConstructor = "new".equals(functionName);
                        boolean isStatic = entry.find("LITERAL_STATIC") != null;

                        if(isConstructor && isStatic) {
                            throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "'static' modifier not allowed here", entry.find("LITERAL_STATIC"), classObject.definitionContext);
                        }

                        Symbol.SymbolVisibility memberVisibility = CommonParsers.parseVisibility(entry.find("SYMBOL_VISIBILITY"), classObject.definitionContext, Symbol.SymbolVisibility.LOCAL);
                        final TokenPattern<?> entryFinal = entry;

                        ArrayList<TridentUserMethodBranch> branches = new ArrayList<>();

                        TokenPattern<?> choice = ((TokenStructure)entry.find("CLASS_FUNCTION_SPLIT")).getContents();
                        switch(choice.getName()) {
                            case "DYNAMIC_FUNCTION": {
                                branches.add(TridentUserMethodBranch.parseDynamicFunction(choice, ctx));
                                break;
                            }
                            case "OVERLOADED_FUNCTION": {
                                TokenList implementations = (TokenList) choice.find("OVERLOADED_FUNCTION_IMPLEMENTATIONS");
                                for(TokenPattern<?> branch : implementations.getContents()) {
                                    branches.add(TridentUserMethodBranch.parseDynamicFunction(branch, ctx));
                                }
                                break;
                            }
                            default:
                                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + choice.getName() + "'", choice, ctx);
                        }

                        if(!isConstructor) {
                            Function<CustomClassObject, Symbol> memberSupplier = thiz -> {
                                ISymbolContext innerFrame = new SymbolContext(classObject.innerContext);
                                if(thiz != null) {
                                    for(Symbol instanceSym : thiz.instanceMembers.values()) {
                                        innerFrame.put(instanceSym);
                                    }
                                }

                                TridentUserMethod method = new TridentUserMethod(branches, innerFrame, thiz, functionName);
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
                            classObject.constructorSupplier = thiz -> new TridentUserMethod(branches, classObject.innerContext, thiz, functionName);
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
    public Object cast(CustomClass object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
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
