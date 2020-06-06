package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperandType;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperationOrder;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.Operator;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperatorHandler;
import com.energyxxer.trident.compiler.semantics.BinaryExpression;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.ParameterizedMemberHolder;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

public class InterpolationManager {

    private static Object nextThis = null;
    private static String nextFunctionName = null;

    public static void setNextFunctionName(String nextFunctionName) {
        InterpolationManager.nextFunctionName = nextFunctionName;
    }

    @Contract("null, _ -> null")
    public static TypeHandler parseType(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        return parse(pattern, ctx, false, TypeHandler.class);
    }

    public static <T> T parse(TokenPattern<?> pattern, ISymbolContext ctx, Class<T> expected) {
        return parse(pattern, ctx, false, expected);
    }

    public static <T> T parse(TokenPattern<?> pattern, ISymbolContext ctx, boolean nullable, Class<T> expected) {
        Object obj = parse(pattern, ctx, false, (Supplier<ActualParameterList>) null);
        if(nullable && obj == null) return null;
        return TridentFunction.HelperMethods.assertOfClass(obj, pattern, ctx, expected);
    }

    public static Object parse(TokenPattern<?> pattern, ISymbolContext ctx, Class... expected) {
        return parse(pattern, ctx, false, expected);
    }

    public static Object parse(TokenPattern<?> pattern, ISymbolContext ctx, boolean nullable, Class... expected) {
        Object obj = parse(pattern, ctx, false, (Supplier<ActualParameterList>) null);
        if(nullable && obj == null) return null;
        return TridentFunction.HelperMethods.assertOfClass(obj, pattern, ctx, expected);
    }

    @Nullable
    public static Object parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        return parse(pattern, ctx, false, (Supplier<ActualParameterList>) null);
    }

    @Nullable
    public static Object parse(TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return parse(pattern, ctx, keepSymbol, (Supplier<ActualParameterList>) null);
    }

    @Nullable
    private static Object parse(TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol, Supplier<ActualParameterList> followingFunctionParams) {
        while (true) {
            if (pattern == null) return null;
            //TridentCompiler compiler = file.getCompiler();
            switch (pattern.getName()) {
                case "INTERPOLATION_BLOCK":
                case "LINE_SAFE_INTERPOLATION_VALUE":
                case "INTERPOLATION_TYPE":
                case "ROOT_INTERPOLATION_TYPE":
                case "INTERPOLATION_VALUE":
                case "MID_INTERPOLATION_VALUE":
                case "ROOT_INTERPOLATION_VALUE": {
                    pattern = ((TokenStructure) pattern).getContents();
                    continue;
                }
                case "VARIABLE": {
                    pattern = pattern.find("VARIABLE_NAME");
                    continue;
                }
                case "INTERPOLATION_WRAPPER":
                case "PARENTHESIZED_VALUE": {
                    pattern = pattern.find("INTERPOLATION_VALUE");
                    continue;
                }
                case "VARIABLE_NAME": {
                    Symbol symbol = ctx.search(pattern.flatten(false), ctx, followingFunctionParams != null ? followingFunctionParams.get() : null);
                    if (symbol == null) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' is not defined", pattern, ctx);
                    }
                    return keepSymbol || symbol instanceof ClassMethodFamily.ClassMethodSymbol ? symbol : sanitizeObject(symbol.getValue(pattern, ctx));
                }
                case "RAW_INTEGER": {
                    try {
                        String raw = pattern.flatten(false);
                        if(raw.toLowerCase().startsWith("0x")) {
                            long asLong = Long.parseLong(raw.substring(2), 16);
                            if(Long.highestOneBit(asLong) <= (long)Integer.MAX_VALUE+1) {
                                return (int) asLong;
                            }
                            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Integer out of range", pattern, ctx);
                        } else if(raw.toLowerCase().startsWith("0b")) {
                            long asLong = Long.parseLong(raw.substring(2), 2);
                            if(Long.highestOneBit(asLong) <= (long)Integer.MAX_VALUE+1) {
                                return (int) asLong;
                            }
                            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Integer out of range", pattern, ctx);
                        } else {
                            return Integer.parseInt(raw);
                        }
                    } catch (NumberFormatException x) {
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
                case "WRAPPED_ROTATION": {
                    return CoordinateParser.parseRotation(pattern.find("ROTATION"), ctx);
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
                    return parse(pattern.find("INTERPOLATION_TYPE"), ctx, false, TypeHandler.class);
                }
                case "DICTIONARY": {
                    DictionaryObject dict = new DictionaryObject();

                    TokenList entryList = (TokenList) pattern.find("DICTIONARY_ENTRY_LIST");

                    if (entryList != null) {
                        for (TokenPattern<?> entry : entryList.searchByName("DICTIONARY_ENTRY")) {
                            String key = entry.find("DICTIONARY_KEY").flatten(false);
                            if (key.startsWith("\"")) {
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
                    TokenPattern<?> choice = ((TokenStructure)pattern.find("NEW_FUNCTION_SPLIT")).getContents();
                    switch(choice.getName()) {
                        case "ANONYMOUS_INNER_FUNCTION": {
                            TridentFile innerFile = TridentFile.createInnerFile(choice.find("ANONYMOUS_INNER_FUNCTION"), ctx);
                            return innerFile.getResourceLocation();
                        }
                        case "DYNAMIC_FUNCTION": {
                            return new TridentUserFunction(nextFunctionName, TridentFunctionBranch.parseDynamicFunction(choice, ctx), ctx, nextThis);
                        }
                        case "OVERLOADED_FUNCTION": {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Overloaded functions should have been removed", choice, ctx);
                        }
                        default:
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + choice.getName() + "'", choice, ctx);
                    }
                }
                case "PRIMITIVE_ROOT_TYPE": {
                    return TridentTypeManager.getPrimitiveHandlerForShorthand(pattern.flatten(false));
                }
                case "NULL_VALUE": {
                    return null;
                }
                case "INTERPOLATION_CHAIN": {
                    return parseAccessorChain(pattern, ctx, keepSymbol);
                }
                case "CONSTRUCTOR_CALL": {
                    TypeHandler<?> handler = parseType(pattern.find("INTERPOLATION_TYPE"), ctx);

                    TridentFunction constructor = handler.getConstructor(pattern, ctx);

                    if (constructor == null) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "There is no constructor for type '" + TridentTypeManager.getTypeIdentifierForType(handler) + "'", pattern.find("INTERPOLATION_TYPE"), ctx);
                    }
                    ArrayList<Object> params = new ArrayList<>();
                    ArrayList<TokenPattern<?>> patterns = new ArrayList<>();

                    TokenList paramList = (TokenList) pattern.find("PARAMETERS");

                    if (paramList != null) {
                        for (TokenPattern<?> rawParam : paramList.getContents()) {
                            if (rawParam.getName().equals("INTERPOLATION_VALUE")) {
                                params.add(parse(rawParam, ctx, keepSymbol));
                                patterns.add(rawParam);
                            }
                        }
                    }

                    return sanitizeObject(constructor.safeCall(params.toArray(), patterns.toArray(new TokenPattern<?>[0]), pattern, ctx));
                }
                case "CAST": {
                    Object parent = parse(pattern.find("MID_INTERPOLATION_VALUE"), ctx);
                    TypeHandler targetType = parseType(pattern.find("INTERPOLATION_TYPE"), ctx);
                    return cast(parent, targetType, pattern, ctx);
                }
                case "SURROUNDED_INTERPOLATION_VALUE": {
                    if (pattern.find("PREFIX_OPERATORS") == null && pattern.find("POSTFIX_OPERATORS") == null) {
                        pattern = pattern.find("INTERPOLATION_CHAIN");
                        continue;
                    }

                    boolean keepValueSymbol = false;
                    TokenList rawPrefixList = (TokenList) pattern.find("PREFIX_OPERATORS");
                    TokenList rawPostfixList = (TokenList) pattern.find("POSTFIX_OPERATORS");

                    ArrayList<Operator> prefixList = new ArrayList<>();
                    ArrayList<Operator> postfixList = new ArrayList<>();

                    if (rawPrefixList != null) {
                        for (TokenPattern<?> rawOp : rawPrefixList.getContents()) {
                            prefixList.add(0, Operator.getOperatorForSymbol(rawOp.flatten(false), true));
                        }
                    }
                    if (rawPostfixList != null) {
                        for (TokenPattern<?> rawOp : rawPostfixList.getContents()) {
                            postfixList.add(Operator.getOperatorForSymbol(rawOp.flatten(false), true));
                        }
                    }

                    if (!prefixList.isEmpty())
                        keepValueSymbol = prefixList.get(0).getLeftOperandType() == OperandType.VARIABLE;
                    else if (!postfixList.isEmpty())
                        keepValueSymbol = postfixList.get(0).getLeftOperandType() == OperandType.VARIABLE;

                    Object value = parse(pattern.find("INTERPOLATION_CHAIN"), ctx, keepValueSymbol);

                    int i = 0;
                    for (Operator op : prefixList) {
                        value = OperatorHandler.Static.perform(null, op, value, rawPrefixList.getContents()[prefixList.size() - 1 - i], ctx);
                        i++;
                    }
                    i = 0;
                    for (Operator op : postfixList) {
                        value = OperatorHandler.Static.perform(value, op, null, rawPostfixList.getContents()[i], ctx);
                        i++;
                    }

                    return value;
                }
                case "EXPRESSION": {
                    //region Expression evaluation
                    TokenList list = (TokenList) pattern;

                    if (list.size() == 1) {
                        pattern = list.getContents()[0];
                        continue;
                    } else {

                        ArrayList<TokenPattern<?>> contents = new ArrayList<>(Arrays.asList(list.getContents()));

                        ArrayList<Object> flatValues = new ArrayList<>();
                        ArrayList<Operator> flatOperators = new ArrayList<>();

                        for (int i = 0; i < contents.size(); i++) {
                            if ((i & 1) == 0) {
                                //Operand
                                flatValues.add(contents.get(i));
                            } else {
                                //Operator
                                Operator operator = Operator.getOperatorForSymbol(contents.get(i).flatten(false));
                                flatOperators.add(operator);
                            }
                        }

                        while (flatOperators.size() >= 1) {
                            int index = -1;
                            Operator topOperator = null;

                            for (int i = 0; i < flatOperators.size(); i++) {
                                Operator op = flatOperators.get(i);
                                if (topOperator == null) {
                                    index = i;
                                    topOperator = op;
                                } else if (topOperator.getPrecedence() >= op.getPrecedence()) {

                                    if (topOperator.getPrecedence() > op.getPrecedence() || topOperator.getOrder() == OperationOrder.RTL) {
                                        index = i;
                                        topOperator = op;
                                    }
                                }
                            }

                            Object a = flatValues.get(index);
                            Object b = flatValues.get(index + 1);

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
    }

    private static TokenPattern<?> sanitizeMemberAccessPattern(@NotNull TokenPattern<?> pattern) {
        while(pattern.getName().equals("MEMBER_ACCESS")) {
            pattern = ((TokenStructure) pattern).getContents();
        }
        return pattern;
    }

    private static Object parseAccessorChain(TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        TokenPattern<?> toBlame = pattern.find("ROOT_INTERPOLATION_VALUE");
        TokenList accessorList = (TokenList) pattern.find("MEMBER_ACCESSES");

        if (accessorList != null) {
            TokenPattern<?>[] accessors = accessorList.getContents();
            for(int i = 0; i < accessors.length; i++) {
                accessors[i] = sanitizeMemberAccessPattern(accessors[i]);
            }

            ActualParameterList[] firstAccessorParameters = new ActualParameterList[] {null};

            Object parent = parse(toBlame, ctx, false, () -> {
                TokenPattern<?> firstAccessor = accessors[0];
                if(firstAccessor.getName().equals("METHOD_CALL")) {
                    ActualParameterList parameterList = parseActualParameterList(firstAccessor, ctx);
                    firstAccessorParameters[0] = parameterList;
                    return parameterList;
                }
                return null;
            });

            if(firstAccessorParameters[0] != null) {
                //Evaluated first accessor, gotta be a function

                if(parent instanceof ClassMethodFamily.ClassMethodSymbol) {
                    parent = sanitizeObject(((ClassMethodFamily.ClassMethodSymbol) parent).safeCall(firstAccessorParameters[0].getValues().toArray(), firstAccessorParameters[0].getPatterns().toArray(new TokenPattern<?>[0]), accessors[0], ctx));
                    toBlame = accessors[0];
                } else if (parent instanceof TridentFunction) {
                    parent = sanitizeObject(((TridentFunction) parent).safeCall(firstAccessorParameters[0].getValues().toArray(), firstAccessorParameters[0].getPatterns().toArray(new TokenPattern<?>[0]), accessors[0], ctx));
                    toBlame = accessors[0];
                } else {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "This is not a function", toBlame, ctx);
                }
            }

            for (int i = firstAccessorParameters[0] == null ? 0 : 1; i < accessors.length; i++) {
                TokenPattern<?> accessor = accessors[i];
                EObject.assertNotNull(parent, toBlame, ctx);

                if(parent instanceof ParameterizedMemberHolder && i+1 < accessors.length && accessor.getName().equals("MEMBER_KEY") && accessors[i+1].getName().equals("METHOD_CALL")) {
                    ActualParameterList paramList = parseActualParameterList(accessors[i+1], ctx);

                    Object member = ((ParameterizedMemberHolder) parent).getMemberForParameters(accessor.find("SYMBOL_NAME").flatten(false), accessor, paramList, ctx, false);

                    EObject.assertNotNull(member, toBlame, ctx);

                    if(member instanceof ClassMethodFamily.ClassMethodSymbol) {
                        parent = sanitizeObject(((ClassMethodFamily.ClassMethodSymbol) member).safeCall(paramList.getValues().toArray(), paramList.getPatterns().toArray(new TokenPattern<?>[0]), accessor, ctx));
                        toBlame = accessor;
                    } else if (member instanceof TridentFunction) {
                        parent = sanitizeObject(((TridentFunction) member).safeCall(paramList.getValues().toArray(), paramList.getPatterns().toArray(new TokenPattern<?>[0]), accessor, ctx));
                        toBlame = accessor;
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "This is not a function", toBlame, ctx);
                    }

                    i++;
                    continue;
                }

                parent = parseAccessor(parent, toBlame, accessor, ctx, keepSymbol && (i == accessors.length-1));
                toBlame = accessor;
            }

            return parent;
        }

        TokenPattern<?> rawTail = pattern.find("INTERPOLATION_CHAIN_TAIL");
        if(rawTail != null) {
            TypeHandler<?> type = parseType(rawTail.find("INTERPOLATION_TYPE"), ctx);

            switch(((TokenStructure) rawTail).getContents().getName()) {
                case "INTERPOLATION_CHAIN_TAIL_IS":
                    return type.isInstance(parse(toBlame, ctx, false));
                case "INTERPOLATION_CHAIN_TAIL_AS":
                    return castOrCoerce(parse(toBlame, ctx, false), type, pattern, ctx, false);
                default:
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + ((TokenStructure)rawTail).getContents().getName() + "'", rawTail, ctx);
            }
        }
        return parse(toBlame, ctx, keepSymbol);
    }

    @SuppressWarnings("unchecked")
    private static Object parseAccessor(Object parent, TokenPattern<?> parentPattern, TokenPattern<?> accessorPattern, ISymbolContext ctx, boolean keepSymbol) {
        //expect sanitized accessor pattern
        EObject.assertNotNull(parent, parentPattern, ctx);
        switch (accessorPattern.getName()) {
            case "MEMBER_KEY": {
                String memberName = accessorPattern.find("SYMBOL_NAME").flatten(false);
                TypeHandler handler = getHandlerForObject(parent, parentPattern, ctx);
                while (true) {
                    try {
                        return sanitizeObject(handler.getMember(parent, memberName, accessorPattern, ctx, keepSymbol));
                    } catch (MemberNotFoundException x) {
                        if ((handler = handler.getSuperType()) == null) {
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot resolve member '" + memberName + "' of " + TridentTypeManager.getTypeIdentifierForObject(parent), accessorPattern, ctx);
                        }
                    }
                }
            }
            case "MEMBER_INDEX": {
                Object index = parse(accessorPattern.find("INDEX.INTERPOLATION_VALUE"), ctx);
                TypeHandler handler = getHandlerForObject(parent, parentPattern, ctx);
                while (true) {
                    try {
                        return sanitizeObject(handler.getIndexer(parent, index, accessorPattern, ctx, keepSymbol));
                    } catch (MemberNotFoundException x) {
                        if ((handler = handler.getSuperType()) == null) {
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot resolve member for index " + index + " of " + TridentTypeManager.getTypeIdentifierForObject(parent), accessorPattern, ctx);
                        }
                    }
                }
            }
            case "METHOD_CALL": {
                if (parent instanceof TridentFunction) {
                    ActualParameterList paramList = parseActualParameterList(accessorPattern, ctx);

                    return sanitizeObject(((TridentFunction) parent).safeCall(paramList.getValues().toArray(), paramList.getPatterns().toArray(new TokenPattern<?>[0]), accessorPattern, ctx));
                } else {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "This is not a function", parentPattern, ctx);
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + accessorPattern.getName() + "'", accessorPattern, ctx);
            }
        }
    }

    private static ActualParameterList parseActualParameterList(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenList paramList = (TokenList) pattern.find("PARAMETERS");
        if(paramList == null) return new ActualParameterList(Collections.emptyList(), Collections.emptyList(), pattern);
        ArrayList<Object> params = new ArrayList<>();
        ArrayList<TokenPattern<?>> patterns = new ArrayList<>();
        for (TokenPattern<?> rawParam : paramList.getContents()) {
            if (rawParam.getName().equals("INTERPOLATION_VALUE")) {
                params.add(parse(rawParam, ctx, false));
                patterns.add(rawParam);
            }
        }
        return new ActualParameterList(params, patterns, pattern);
    }

    @Contract("null, _, _, _ -> null")
    public static Object cast(Object obj, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return cast(obj, targetType, pattern, ctx, true);
    }

    @Contract("null, _, _, _, _ -> null")
    public static Object cast(Object obj, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx, boolean failureException) {
        if(obj == null) return null;
        if("primitive(string)".equals(TridentTypeManager.getInternalTypeIdentifierForType(targetType))) {
            return castToString(obj, pattern, ctx);
        }
        if(targetType.isInstance(obj)) return obj;
        TypeHandler sourceType = getHandlerForObject(obj, pattern, ctx);
        try {
            return sourceType.cast(obj, targetType, pattern, ctx);
        } catch(ClassCastException x) {
            if(failureException) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Unable to cast " + TridentTypeManager.getTypeIdentifierForObject(obj) + " to type " + targetType.getTypeIdentifier(), pattern, ctx);
            }
            return null;
        }
    }

    @Contract("null, _, _, _ -> null")
    public static Object castOrCoerce(Object obj, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return castOrCoerce(obj, targetType, pattern, ctx, true);
    }

    @Contract("null, _, _, _, _ -> null")
    public static Object castOrCoerce(Object obj, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx, boolean failureException) {
        if(obj == null) return null;
        Object cast = cast(obj, targetType, pattern, ctx, false);
        if(cast != null) return cast;

        TypeHandler sourceType = getHandlerForObject(obj, pattern, ctx);
        if(sourceType.canCoerce(obj, targetType)) {
            return sourceType.coerce(obj, targetType, pattern, ctx);
        }
        if(failureException) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Unable to cast or coerce " + TridentTypeManager.getTypeIdentifierForObject(obj) + " to type " + targetType.getTypeIdentifier(), pattern, ctx);
        }
        return null;
    }

    public static TypeHandler getHandlerForObject(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        TypeHandler handler = TridentTypeManager.getHandlerForObject(value);
        if(handler == null) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Couldn't find handler for type " + value.getClass().getName(), pattern, ctx);
        }
        return handler;
    }

    @Contract("null -> null")
    private static Object sanitizeObject(Object obj) {
        if(obj == null) return null;
        if(obj.getClass().isArray()) {
            return new ListObject((Object[]) obj);
        }
        return obj;
    }

    public static String castToString(Object obj) {
        if(obj instanceof TridentFunction && !(obj instanceof TridentUserFunction)) {
            return "<internal function>";
        } else if(obj instanceof TypeHandler && ((TypeHandler) obj).isStaticHandler()) {
            return "type_definition<" + ((TypeHandler) obj).getTypeIdentifier() + ">";
        } else {
            return String.valueOf(obj);
        }
    }

    public static String castToString(Object obj, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(obj instanceof TridentFunction && !(obj instanceof TridentUserFunction)) {
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
