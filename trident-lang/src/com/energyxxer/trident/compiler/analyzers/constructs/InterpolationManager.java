package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperandType;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperationOrder;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.Operator;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperatorHandler;
import com.energyxxer.trident.compiler.semantics.BinaryExpression;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class InterpolationManager {

    private static Object nextThis = null;
    private static String nextFunctionName = null;

    public static void setNextFunctionName(String nextFunctionName) {
        InterpolationManager.nextFunctionName = nextFunctionName;
    }

    public static TypeHandler parseType(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        return parse(pattern, ctx, false, TypeHandler.class);
    }

    public static <T> T parse(TokenPattern<?> pattern, ISymbolContext ctx, Class<T> expected) {
        return parse(pattern, ctx, false, expected);
    }

    @SuppressWarnings("unchecked")
    public static <T> T parse(TokenPattern<?> pattern, ISymbolContext ctx, boolean nullable, Class<T> expected) {
        Object obj = parse(pattern, ctx, false);
        if(nullable && obj == null) return null;
        if(expected.isInstance(obj)) {
            return (T) obj;
        } else {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type " + expected.getSimpleName(), pattern, ctx);
        }
    }

    public static Object parse(TokenPattern<?> pattern, ISymbolContext ctx, Class... expected) {
        return parse(pattern, ctx, false, expected);
    }

    public static Object parse(TokenPattern<?> pattern, ISymbolContext ctx, boolean nullable, Class... expected) {
        Object obj = parse(pattern, ctx, false);
        if(nullable && obj == null) return null;
        for(Class cls : expected) {
            if(cls.isInstance(obj)) return obj;
        }
        throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type " + Arrays.stream(expected).map(Class::getSimpleName).collect(Collectors.joining(", ")), pattern, ctx);
    }

    @Nullable
    public static Object parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        return parse(pattern, ctx, false);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static Object parse(TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(pattern == null) return null;
        //TridentCompiler compiler = file.getCompiler();
        switch(pattern.getName()) {
            case "INTERPOLATION_BLOCK":
            case "LINE_SAFE_INTERPOLATION_VALUE":
            case "INTERPOLATION_TYPE":
            case "ROOT_INTERPOLATION_TYPE":
            case "INTERPOLATION_VALUE":
            case "MID_INTERPOLATION_VALUE":
            case "ROOT_INTERPOLATION_VALUE": {
                return parse(((TokenStructure) pattern).getContents(), ctx, keepSymbol);
            }
            case "VARIABLE": {
                return parse(pattern.find("VARIABLE_NAME"), ctx, keepSymbol);
            }
            case "INTERPOLATION_WRAPPER":
            case "PARENTHESIZED_VALUE": {
                return parse(pattern.find("INTERPOLATION_VALUE"), ctx, keepSymbol);
            }
            case "VARIABLE_NAME": {
                Symbol symbol = ctx.search(pattern.flatten(false), ctx);
                if(symbol == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' is not defined", pattern, ctx);
                }
                return keepSymbol ? symbol : sanitizeObject(symbol.getValue());
            }
            case "RAW_INTEGER": {
                try {
                    return Integer.parseInt(pattern.flatten(false));
                } catch(NumberFormatException x) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Integer out of range", pattern, ctx);
                }
            }
            case "RAW_REAL": {
                return Double.parseDouble(pattern.flatten(false));
            }
            case "BOOLEAN": {
                return pattern.flatten(false).equals("true");
            }
            case "STRING_LITERAL": {
                return CommonParsers.parseQuotedString(pattern.flatten(false), pattern, ctx);
            }
            case "WRAPPED_ENTITY": {
                return EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
            }
            case "WRAPPED_BLOCK": {
                return CommonParsers.parseBlock(pattern.find("BLOCK_TAGGED"), ctx);
            }
            case "WRAPPED_ITEM": {
                return CommonParsers.parseItem(pattern.find("ITEM_TAGGED"), ctx, NBTMode.SETTING);
            }
            case "WRAPPED_TEXT_COMPONENT": {
                return TextParser.parseTextComponent(pattern.find("TEXT_COMPONENT"), ctx);
            }
            case "WRAPPED_NBT": {
                return NBTParser.parseCompound(pattern.find("NBT_COMPOUND"), ctx);
            }
            case "WRAPPED_NBT_VALUE": {
                return NBTParser.parseValue(pattern.find("NBT_VALUE"), ctx);
            }
            case "WRAPPED_NBT_PATH": {
                return NBTParser.parsePath(pattern.find("NBT_PATH"), ctx);
            }
            case "WRAPPED_COORDINATE": {
                return CoordinateParser.parse(pattern.find("COORDINATE_SET"), ctx);
            }
            case "WRAPPED_INT_RANGE": {
                return CommonParsers.parseIntRange(pattern.find("INTEGER_NUMBER_RANGE"), ctx);
            }
            case "WRAPPED_REAL_RANGE": {
                return CommonParsers.parseRealRange(pattern.find("REAL_NUMBER_RANGE"), ctx);
            }
            case "WRAPPED_RESOURCE": {
                return CommonParsers.parseResourceLocation(pattern.find("RESOURCE_LOCATION_TAGGED"), ctx);
            }
            case "WRAPPED_POINTER": {
                return CommonParsers.parsePointer(pattern.find("POINTER"), ctx);
            }
            case "WRAPPED_TYPE": {
                return parse(pattern.find("INTERPOLATION_TYPE"), ctx, keepSymbol);
            }
            case "DICTIONARY": {
                DictionaryObject dict = new DictionaryObject();

                TokenList entryList = (TokenList) pattern.find("DICTIONARY_ENTRY_LIST");

                if(entryList != null) {
                    for(TokenPattern<?> entry : entryList.searchByName("DICTIONARY_ENTRY")) {
                        String key = entry.find("DICTIONARY_KEY").flatten(false);
                        if(key.startsWith("\"")) {
                            key = CommonParsers.parseQuotedString(key, pattern, ctx);
                        }
                        nextThis = dict;
                        nextFunctionName = key;
                        Object value = parse(entry.find("INTERPOLATION_VALUE"), ctx, keepSymbol);
                        nextThis = null;
                        nextFunctionName = null;
                        dict.put(key, value);
                    }
                }

                return dict;
            }
            case "LIST": {
                ListObject list = new ListObject();

                TokenList entryList = (TokenList) pattern.find("LIST_ENTRIES");

                if (entryList != null) {
                    for (TokenPattern<?> entry : entryList.searchByName("INTERPOLATION_VALUE")) {
                        nextThis = list;
                        list.add(parse(entry, ctx));
                        nextThis = null;
                    }
                }
                return list;
            }
            case "NEW_FUNCTION": {
                if(pattern.find("FORMAL_PARAMETERS_OPT") != null) {
                    ArrayList<FormalParameter> formalParams = new ArrayList<>();
                    TokenList paramNames = (TokenList) pattern.find("FORMAL_PARAMETERS_OPT.FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST");
                    if(paramNames != null) {
                        for(TokenPattern<?> param : paramNames.searchByName("FORMAL_PARAMETER")) {
                            formalParams.add(new FormalParameter(param.find("FORMAL_PARAMETER_NAME").flatten(false), TypeConstraints.parseConstraints(param.find("TYPE_CONSTRAINTS"), ctx)));
                        }
                    }

                    return new TridentUserMethod(pattern.find("ANONYMOUS_INNER_FUNCTION"), ctx, formalParams, nextThis, nextFunctionName);
                } else {
                    TridentFile innerFile = TridentFile.createInnerFile(pattern.find("ANONYMOUS_INNER_FUNCTION"), ctx);
                    return innerFile.getResourceLocation();
                }
            }
            case "PRIMITIVE_ROOT_TYPE": {
                return TridentTypeManager.getHandlerForShorthand(pattern.flatten(false));
            }
            case "NULL_VALUE": {
                return null;
            }
            case "INTERPOLATION_CHAIN": {
                TokenPattern<?> toBlame = pattern.find("ROOT_INTERPOLATION_VALUE");
                TokenList accessors = (TokenList) pattern.find("MEMBER_ACCESSES");
                Object parent = parse(toBlame, ctx, keepSymbol && accessors == null);

                if(accessors != null) {
                    int i = accessors.getContents().length-1;
                    for(TokenPattern<?> accessor : accessors.getContents()) {
                        EObject.assertNotNull(parent, toBlame, ctx);
                        parent = parseAccessor(parent, toBlame, accessor, ctx, keepSymbol && i == 0);
                        toBlame = accessor;
                        i--;
                    }
                }

                return parent;
            }
            case "CONSTRUCTOR_CALL": {
                TypeHandler<?> handler = parseType(pattern.find("INTERPOLATION_TYPE"), ctx);

                TridentMethod constructor = handler.getConstructor();
                //ObjectConstructors.getConstructor(constructorName);
                if(constructor == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "There is no constructor for type '" + TridentTypeManager.getTypeIdentifierForType(handler) + "'", pattern.find("INTERPOLATION_TYPE"), ctx);
                }
                ArrayList<Object> params = new ArrayList<>();
                ArrayList<TokenPattern<?>> patterns = new ArrayList<>();

                TokenList paramList = (TokenList) pattern.find("PARAMETERS");

                if(paramList != null) {
                    for(TokenPattern<?> rawParam : paramList.getContents()) {
                        if(rawParam.getName().equals("INTERPOLATION_VALUE")) {
                            params.add(parse(rawParam, ctx, keepSymbol));
                            patterns.add(rawParam);
                        }
                    }
                }

                return sanitizeObject(constructor.safeCall(params.toArray(), patterns.toArray(new TokenPattern<?>[0]), pattern, ctx));
            }
            case "CAST": {
                Object parent = parse(pattern.find("MID_INTERPOLATION_VALUE"), ctx);
                Class newType = TridentTypeManager.getHandlerForShorthand(pattern.find("TARGET_TYPE").flatten(false)).getHandledClass();
                return cast(parent, newType, pattern, ctx);
            }
            case "SURROUNDED_INTERPOLATION_VALUE": {
                if(pattern.find("PREFIX_OPERATORS") == null && pattern.find("POSTFIX_OPERATORS") == null) return parse(pattern.find("INTERPOLATION_CHAIN"), ctx, keepSymbol);

                boolean keepValueSymbol = false;
                TokenList rawPrefixList = (TokenList) pattern.find("PREFIX_OPERATORS");
                TokenList rawPostfixList = (TokenList) pattern.find("POSTFIX_OPERATORS");

                ArrayList<Operator> prefixList = new ArrayList<>();
                ArrayList<Operator> postfixList = new ArrayList<>();

                if(rawPrefixList != null) {
                    for(TokenPattern<?> rawOp : rawPrefixList.getContents()) {
                        prefixList.add(0, Operator.getOperatorForSymbol(rawOp.flatten(false), true));
                    }
                }
                if(rawPostfixList != null) {
                    for(TokenPattern<?> rawOp : rawPostfixList.getContents()) {
                        postfixList.add(Operator.getOperatorForSymbol(rawOp.flatten(false), true));
                    }
                }

                if(!prefixList.isEmpty()) keepValueSymbol = prefixList.get(0).getLeftOperandType() == OperandType.VARIABLE;
                else if(!postfixList.isEmpty()) keepValueSymbol = postfixList.get(0).getLeftOperandType() == OperandType.VARIABLE;

                Object value = parse(pattern.find("INTERPOLATION_CHAIN"), ctx, keepValueSymbol);

                int i = 0;
                for(Operator op : prefixList) {
                    value = OperatorHandler.Static.perform(null, op, value, rawPrefixList.getContents()[prefixList.size()-1-i], ctx);
                    i++;
                }
                i = 0;
                for(Operator op : postfixList) {
                    value = OperatorHandler.Static.perform(value, op, null, rawPostfixList.getContents()[i], ctx);
                    i++;
                }

                return value;
            }
            case "EXPRESSION": {
                //region Expression evaluation
                TokenList list = (TokenList) pattern;

                if(list.size() == 1) {
                    return parse(list.getContents()[0], ctx, keepSymbol);
                } else {

                    ArrayList<TokenPattern<?>> contents = new ArrayList<>(Arrays.asList(list.getContents()));

                    ArrayList<Object> flatValues = new ArrayList<>();
                    ArrayList<Operator> flatOperators = new ArrayList<>();

                    for(int i = 0; i < contents.size(); i++) {
                        if((i & 1) == 0) {
                            //Operand
                            flatValues.add(contents.get(i));
                        } else {
                            //Operator
                            Operator operator = Operator.getOperatorForSymbol(contents.get(i).flatten(false));
                            flatOperators.add(operator);
                        }
                    }

                    while(flatOperators.size() >= 1) {
                        int index = -1;
                        Operator topOperator = null;

                        for(int i = 0; i < flatOperators.size(); i++) {
                            Operator op = flatOperators.get(i);
                            if(topOperator == null) {
                                index = i;
                                topOperator = op;
                            } else if(topOperator.getPrecedence() >= op.getPrecedence()) {

                                if(topOperator.getPrecedence() > op.getPrecedence() || topOperator.getOrder() == OperationOrder.RTL) {
                                    index = i;
                                    topOperator = op;
                                }
                            }
                        }

                        Object a = flatValues.get(index);
                        Object b = flatValues.get(index+1);

                        flatValues.remove(index);
                        flatValues.remove(index);

                        flatValues.add(index, new BinaryExpression(a, topOperator, b, pattern, ctx));
                        flatOperators.remove(index);
                    }

                    return ((BinaryExpression) flatValues.get(0)).evaluate();
                }
                //endregion
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Object parseAccessor(Object parent, TokenPattern<?> parentPattern, TokenPattern<?> accessorPattern, ISymbolContext ctx, boolean keepSymbol) {
        if(accessorPattern.getName().equals("MEMBER_ACCESS")) return parseAccessor(parent, parentPattern, ((TokenStructure) accessorPattern).getContents(), ctx, keepSymbol);
        EObject.assertNotNull(parent, parentPattern, ctx);
        switch(accessorPattern.getName()) {
            case "MEMBER_KEY": {
                String memberName = accessorPattern.find("MEMBER_NAME").flatten(false);
                TypeHandler handler = getHandlerForObject(parent, parentPattern, ctx);
                while(true) {
                    try {
                        return sanitizeObject(handler.getMember(parent, memberName, accessorPattern, ctx, keepSymbol));
                    } catch(MemberNotFoundException x) {
                        if((handler = handler.getSuperType()) == null) {
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot resolve member '" + memberName + "' of " + TridentTypeManager.getTypeIdentifierForObject(parent), accessorPattern, ctx);
                        }
                    }
                }
            }
            case "MEMBER_INDEX": {
                Object index = parse(accessorPattern.find("INDEX.INTERPOLATION_VALUE"), ctx);
                TypeHandler handler = getHandlerForObject(parent, parentPattern, ctx);
                while(true) {
                    try {
                        return sanitizeObject(handler.getIndexer(parent, index, accessorPattern, ctx, keepSymbol));
                    } catch(MemberNotFoundException x) {
                        if((handler = handler.getSuperType()) == null) {
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot resolve member for index " + index + " of " + TridentTypeManager.getTypeIdentifierForObject(parent), accessorPattern, ctx);
                        }
                    }
                }
            }
            case "METHOD_CALL": {
                if(parent instanceof TridentMethod) {

                    ArrayList<Object> params = new ArrayList<>();
                    ArrayList<TokenPattern<?>> patterns = new ArrayList<>();

                    TokenList paramList = (TokenList) accessorPattern.find("PARAMETERS");

                    if(paramList != null) {
                        for(TokenPattern<?> rawParam : paramList.getContents()) {
                            if(rawParam.getName().equals("INTERPOLATION_VALUE")) {
                                params.add(parse(rawParam, ctx, keepSymbol));
                                patterns.add(rawParam);
                            }
                        }
                    }

                    return sanitizeObject(((TridentMethod) parent).safeCall(params.toArray(), patterns.toArray(new TokenPattern<?>[0]), accessorPattern, ctx));
                } else {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "This is not a method", parentPattern, ctx);
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + accessorPattern.getName() + "'", accessorPattern, ctx);
            }
        }
    }

    //TODO: Rework this to use variable type handlers instead of classes
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj, Class<T> newType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(obj == null) return null;
        if(newType == String.class) {
            return (T)castToString(obj, pattern, ctx);
        }
        TypeHandler handler = getHandlerForObject(obj, pattern, ctx);
        if(newType == obj.getClass()) return (T) obj;
        try {
            return (T) handler.cast(obj, newType, pattern, ctx);
        } catch(ClassCastException x) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Unable to cast " + obj.getClass().getSimpleName() + " to type " + newType.getName(), pattern, ctx);
        }
    }

    public static TypeHandler getHandlerForObject(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        TypeHandler handler = TridentTypeManager.getHandlerForObject(value);
        if(handler == null) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Couldn't find handler for type " + value.getClass().getName(), pattern, ctx);
        }
        return handler;
    }

    public static Object sanitizeObject(Object obj) {
        if(obj == null) return null;
        if(obj.getClass().isArray()) {
            return new ListObject((Object[]) obj);
        }
        return obj;
    }

    public static String castToString(Object obj) {
        if(obj instanceof TridentMethod && !(obj instanceof TridentUserMethod)) {
            return "<internal function>";
        } else if(obj instanceof TypeHandler && ((TypeHandler) obj).isStaticHandler()) {
            return "type_definition<" + ((TypeHandler) obj).getTypeIdentifier() + ">";
        } else {
            return String.valueOf(obj);
        }
    }

    public static String castToString(Object obj, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(obj instanceof TridentMethod && !(obj instanceof TridentUserMethod)) {
            return "<internal function>";
        } else if(obj instanceof TypeHandler && ((TypeHandler) obj).isStaticHandler()) {
            return "type_definition<" + ((TypeHandler) obj).getTypeIdentifier() + ">";
        } else if(obj instanceof ContextualToString) {
            return ((ContextualToString) obj).contextualToString(pattern, ctx);
        } else {
            return String.valueOf(obj);
        }
    }
}
