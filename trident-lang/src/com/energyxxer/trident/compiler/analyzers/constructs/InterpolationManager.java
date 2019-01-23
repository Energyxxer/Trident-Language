package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.constructors.ObjectConstructors;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperationOrder;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.Operator;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperatorHandler;
import com.energyxxer.trident.extensions.EObject;
import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperandType;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class InterpolationManager {

    private static Object nextThis = null;
    private static String nextFunctionName = null;

    @SuppressWarnings("unchecked")
    public static <T> T parse(TokenPattern<?> pattern, TridentFile file, Class<T> expected) {
        Object obj = parse(pattern, file, false);
        if(expected.isInstance(obj)) {
            return (T) obj;
        } else {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type " + expected.getSimpleName(), pattern, file);
        }
    }

    public static Object parse(TokenPattern<?> pattern, TridentFile file, Class... expected) {
        Object obj = parse(pattern, file, false);
        for(Class cls : expected) {
            if(cls.isInstance(obj)) return obj;
        }
        throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type " + Arrays.asList(expected).stream().map(Class::getSimpleName).collect(Collectors.joining(", ")), pattern, file);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static Object parse(TokenPattern<?> pattern, TridentFile file) {
        return parse(pattern, file, false);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static Object parse(TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(pattern == null) return null;
        //TridentCompiler compiler = file.getCompiler();
        switch(pattern.getName()) {
            case "INTERPOLATION_BLOCK": {
                return parse(((TokenStructure) pattern).getContents(), file, keepSymbol);
            }
            case "VARIABLE": {
                return parse(pattern.find("VARIABLE_NAME"), file, keepSymbol);
            }
            case "INTERPOLATION_WRAPPER": {
                return parse(pattern.find("INTERPOLATION_VALUE"), file, keepSymbol);
            }
            case "LINE_SAFE_INTERPOLATION_VALUE":
            case "CLOSED_INTERPOLATION_VALUE": {
                return parse(((TokenStructure) pattern).getContents(), file, keepSymbol);
            }
            case "INTERPOLATION_VALUE": {
                return parse(((TokenStructure) pattern).getContents(), file, keepSymbol);
            }
            case "VARIABLE_NAME": {
                Symbol symbol = file.getCompiler().getSymbolStack().search(pattern.flatten(false), file);
                if(symbol == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' is not defined", pattern, file);
                }
                return keepSymbol ? symbol : sanitizeObject(symbol.getValue());
            }
            case "RAW_INTEGER": {
                return Integer.parseInt(pattern.flatten(false));
            }
            case "RAW_REAL": {
                return Double.parseDouble(pattern.flatten(false));
            }
            case "BOOLEAN": {
                return pattern.flatten(false).equals("true");
            }
            case "STRING_LITERAL": {
                return CommandUtils.parseQuotedString(pattern.flatten(false));
            }
            case "WRAPPED_ENTITY": {
                return EntityParser.parseEntity(pattern.find("ENTITY"), file);
            }
            case "WRAPPED_BLOCK": {
                return CommonParsers.parseBlock(pattern.find("BLOCK_TAGGED"), file);
            }
            case "WRAPPED_ITEM": {
                return CommonParsers.parseItem(pattern.find("ITEM_TAGGED"), file, NBTMode.SETTING);
            }
            case "WRAPPED_TEXT_COMPONENT": {
                return TextParser.parseTextComponent(pattern.find("TEXT_COMPONENT"), file);
            }
            case "WRAPPED_NBT": {
                return NBTParser.parseCompound(pattern.find("NBT_COMPOUND"), file);
            }
            case "WRAPPED_NBT_VALUE": {
                return NBTParser.parseValue(pattern.find("NBT_VALUE"), file);
            }
            case "WRAPPED_NBT_PATH": {
                return NBTParser.parsePath(pattern.find("NBT_PATH"), file);
            }
            case "WRAPPED_COORDINATE": {
                return CoordinateParser.parse(pattern.find("COORDINATE_SET"), file);
            }
            case "WRAPPED_INT_RANGE": {
                return CommonParsers.parseIntRange(pattern.find("INTEGER_NUMBER_RANGE"), file);
            }
            case "WRAPPED_REAL_RANGE": {
                return CommonParsers.parseRealRange(pattern.find("REAL_NUMBER_RANGE"), file);
            }
            case "WRAPPED_RESOURCE": {
                return CommonParsers.parseResourceLocation(pattern.find("RESOURCE_LOCATION_TAGGED"), file);
            }
            case "DICTIONARY": {
                DictionaryObject dict = new DictionaryObject();

                TokenList entryList = (TokenList) pattern.find("DICTIONARY_ENTRY_LIST");

                if(entryList != null) {
                    for(TokenPattern<?> entry : entryList.searchByName("DICTIONARY_ENTRY")) {
                        String key = entry.find("DICTIONARY_KEY").flatten(false);
                        if(key.startsWith("\"")) {
                            key = CommandUtils.parseQuotedString(key);
                        }
                        nextThis = dict;
                        nextFunctionName = key;
                        Object value = parse(entry.find("INTERPOLATION_VALUE"), file, keepSymbol);
                        nextThis = null;
                        nextFunctionName = null;
                        dict.put(key, value);
                    }
                }

                return dict;
            }
            case "LIST": {
                ListType list = new ListType();

                TokenList entryList = (TokenList) pattern.find("LIST_ENTRIES");

                if (entryList != null) {
                    for (TokenPattern<?> entry : entryList.searchByName("INTERPOLATION_VALUE")) {
                        nextThis = list;
                        list.add(parse(entry, file));
                        nextThis = null;
                    }
                }
                return list;
            }
            case "NEW_FUNCTION": {
                if(pattern.find("FORMAL_PARAMETERS") != null) {
                    ArrayList<String> formalParams = new ArrayList<>();
                    TokenList paramNames = (TokenList) pattern.find("FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST");
                    if(paramNames != null) {
                        for(TokenPattern<?> paramName : paramNames.searchByName("FORMAL_PARAMETER_NAME")) {
                            formalParams.add(paramName.flatten(false));
                        }
                    }

                    return new FunctionMethod(pattern.find("ANONYMOUS_INNER_FUNCTION"), file, formalParams, nextThis, nextFunctionName);
                } else {
                    TridentFile innerFile = TridentFile.createInnerFile(pattern.find("ANONYMOUS_INNER_FUNCTION"), file);
                    return innerFile.getResourceLocation();
                }
            }
            case "NULL_VALUE": {
                return null;
            }
            case "MEMBER": {
                Object parent = parse(pattern.find("INTERPOLATION_VALUE"), file);
                EObject.assertNotNull(parent, pattern.find("INTERPOLATION_VALUE"), file);
                VariableTypeHandler handler = getHandlerForObject(parent, pattern, file);
                String memberName = pattern.find("MEMBER_NAME").flatten(false);
                try {
                    return sanitizeObject(handler.getMember(parent, memberName, pattern, file, keepSymbol));
                } catch(MemberNotFoundException x) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot resolve member '" + memberName + "' of " + parent.getClass().getSimpleName(), pattern, file);
                }
            }
            case "INDEXED_MEMBER": {
                Object parent = parse(pattern.find("INTERPOLATION_VALUE"), file);
                EObject.assertNotNull(parent, pattern.find("INTERPOLATION_VALUE"), file);
                VariableTypeHandler handler = getHandlerForObject(parent, pattern, file);
                Object index = parse(pattern.find("INDEX.INTERPOLATION_VALUE"), file);
                try {
                    return sanitizeObject(handler.getIndexer(parent, index, pattern, file, keepSymbol));
                } catch(MemberNotFoundException x) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot resolve member for index " + index + " of " + parent.getClass().getSimpleName(), pattern, file);
                }
            }
            case "METHOD_CALL": {
                Object parent = parse(pattern.find("INTERPOLATION_VALUE"), file);
                EObject.assertNotNull(parent, pattern.find("INTERPOLATION_VALUE"), file);
                if(parent instanceof VariableMethod) {

                    ArrayList<Object> params = new ArrayList<>();
                    ArrayList<TokenPattern<?>> patterns = new ArrayList<>();

                    TokenList paramList = (TokenList) pattern.find("PARAMETERS");

                    if(paramList != null) {
                        for(TokenPattern<?> rawParam : paramList.getContents()) {
                            if(rawParam.getName().equals("INTERPOLATION_VALUE")) {
                                params.add(parse(rawParam, file, keepSymbol));
                                patterns.add(rawParam);
                            }
                        }
                    }

                    return sanitizeObject(((VariableMethod) parent).call(params.toArray(), patterns.toArray(new TokenPattern<?>[0]), pattern, file));
                } else {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "This is not a method", pattern.find("INTERPOLATION_VALUE"), file);
                }
            }
            case "CONSTRUCTOR_CALL": {
                String constructorName = pattern.find("CONSTRUCTOR_NAME").flatten(false);
                VariableMethod constructor = ObjectConstructors.getConstructor(constructorName);
                if(constructor == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "There is no constructor with the name '" + constructorName + "'", pattern.find("CONSTRUCTOR_NAME"), file);
                }
                ArrayList<Object> params = new ArrayList<>();
                ArrayList<TokenPattern<?>> patterns = new ArrayList<>();

                TokenList paramList = (TokenList) pattern.find("PARAMETERS");

                if(paramList != null) {
                    for(TokenPattern<?> rawParam : paramList.getContents()) {
                        if(rawParam.getName().equals("INTERPOLATION_VALUE")) {
                            params.add(parse(rawParam, file, keepSymbol));
                            patterns.add(rawParam);
                        }
                    }
                }

                return sanitizeObject(constructor.call(params.toArray(), patterns.toArray(new TokenPattern<?>[0]), pattern, file));
            }
            case "PARENTHESIZED_VALUE": {
                return parse(pattern.find("INTERPOLATION_VALUE"), file, keepSymbol);
            }
            case "CAST": {
                Object parent = parse(pattern.find("INTERPOLATION_VALUE"), file);
                Class newType = VariableTypeHandler.Static.getClassForShorthand(pattern.find("TARGET_TYPE").flatten(false));
                return cast(parent, newType, pattern, file);
            }
            case "EXPRESSION": {
                //region Expression evaluation
                TokenList list = (TokenList) pattern;

                if(list.size() == 1) {
                    return parse(list.getContents()[0], file, keepSymbol);
                } else {

                    ArrayList<TokenPattern<?>> contents = new ArrayList<>(Arrays.asList(list.getContents()));

                    ArrayList<Object> flatValues = new ArrayList<>();
                    ArrayList<Operator> flatOperators = new ArrayList<>();

                    for(int i = 0; i < contents.size(); i++) {
                        if((i & 1) == 0) {
                            //Operand
                            Object value = parse(contents.get(i), file, keepSymbol);
                            flatValues.add(value);
                        } else {
                            //Operator
                            Operator operator = Operator.getOperatorForSymbol(contents.get(i).flatten(false));
                            if(operator.getLeftOperandType() == OperandType.VARIABLE) {
                                flatValues.remove(flatValues.size()-1);

                                Object symbol = parse(contents.get(i-1), file, true);
                                if(symbol instanceof Symbol) {
                                    flatValues.add(symbol);
                                } else {
                                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected symbol in the left hand side of the expression", pattern, file);
                                }
                            }
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

                        Object result = OperatorHandler.Static.perform(a, topOperator, b, pattern, file);

                        flatValues.add(index, result);
                        flatOperators.remove(index);
                    }

                    return flatValues.get(0);
                }
                //endregion
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, file);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj, Class<T> newType, TokenPattern<?> pattern, TridentFile file) {
        if(obj == null) return null;
        if(newType == String.class) return (T)String.valueOf(obj);
        VariableTypeHandler handler = getHandlerForObject(obj, pattern, file);
        if(newType == obj.getClass()) return (T) obj;
        try {
            return (T) handler.cast(obj, newType, pattern, file);
        } catch(ClassCastException x) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Unable to cast " + obj.getClass().getSimpleName() + " to type " + newType.getName(), pattern, file);
        }
    }

    @NotNull
    public static VariableTypeHandler getHandlerForObject(Object value, TokenPattern<?> pattern, TridentFile file) {
        if(value instanceof VariableTypeHandler) return ((VariableTypeHandler) value);
        if(value == null) return new NullType();
        VariableTypeHandler handler = AnalyzerManager.getAnalyzer(VariableTypeHandler.class, VariableTypeHandler.Static.getIdentifierForClass(value.getClass()));
        if(handler == null) {
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Couldn't find handler for type " + value.getClass().getName(), pattern, file);
        }
        return handler;
    }

    public static Object sanitizeObject(Object obj) {
        if(obj == null) return null;
        if(obj.getClass().isArray()) {
            return new ListType((Object[]) obj);
        }
        return obj;
    }
}
