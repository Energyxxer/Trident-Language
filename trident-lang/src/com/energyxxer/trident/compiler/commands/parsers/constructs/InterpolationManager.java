package com.energyxxer.trident.compiler.commands.parsers.constructs;

import Trident.extensions.java.lang.Object.EObject;
import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.enxlex.pattern_matching.structures.TokenItem;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.*;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperandType;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperationOrder;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.Operator;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperatorHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

public class InterpolationManager {

    @SuppressWarnings("unchecked")
    public static <T> T parse(TokenPattern<?> pattern, TridentFile file, Class<T> expected) {
        Object obj = parse(pattern, file, false);
        if(expected.isInstance(obj)) {
            return (T) obj;
        } else {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type " + expected.getSimpleName(), pattern));
            throw new EntryParsingException();
        }
    }

    public static Object parse(TokenPattern<?> pattern, TridentFile file, Class... expected) {
        Object obj = parse(pattern, file, false);
        for(Class cls : expected) {
            if(cls.isInstance(obj)) return obj;
        }
        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type " + Arrays.asList(expected).map((Class c) -> c.getSimpleName()).toSet().join(", "), pattern));
        throw new EntryParsingException();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static Object parse(TokenPattern<?> pattern, TridentFile file) {
        return parse(pattern, file, false);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static Object parse(TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
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
            case "CLOSED_INTERPOLATION_VALUE": {
                return parse(((TokenStructure) pattern).getContents(), file, keepSymbol);
            }
            case "INTERPOLATION_VALUE": {
                return parse(((TokenStructure) pattern).getContents(), file, keepSymbol);
            }
            case "VARIABLE_NAME": {
                Symbol symbol = file.getCompiler().getStack().search(pattern.flatten(false));
                if(symbol == null) {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + pattern.flatten(false) + "' is not defined", pattern));
                    throw new EntryParsingException();
                }
                return keepSymbol ? symbol : symbol.getValue();
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
                return CommonParsers.parseBlock(pattern.find("ITEM_TAGGED"), file);
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
            case "WRAPPED_DICTIONARY": {
                return parse(pattern.find("DICTIONARY"), file, keepSymbol);
            }
            case "DICTIONARY": {
                DictionaryObject dict = new DictionaryObject();

                TokenList entryList = (TokenList) pattern.find("DICTIONARY_ENTRY_LIST");

                if(entryList != null) {
                    for(TokenPattern<?> entry : entryList.searchByName("DICTIONARY_ENTRY")) {
                        String key = entry.find("DICTIONARY_KEY").flatten(false);
                        Object value = parse(entry.find("INTERPOLATION_VALUE"), file, keepSymbol);
                        dict.put(key, value);
                    }
                }

                return dict;
            }
            case "WRAPPED_LIST": {
                return parse(pattern.find("LIST"), file, keepSymbol);
            }
            case "LIST": {
                ListType list = new ListType();

                TokenList entryList = (TokenList) pattern.find("LIST_ENTRIES");

                if (entryList != null) {
                    for (TokenPattern<?> entry : entryList.searchByName("INTERPOLATION_VALUE")) {
                        list.add(parse(entry, file));
                    }

                    return list;
                }
            }
            case "NEW_FUNCTION": {
                TridentFile innerFile = TridentFile.createInnerFile(pattern.find("ANONYMOUS_INNER_FUNCTION"), file);
                return innerFile.getResourceLocation();
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
                    return handler.getMember(parent, memberName, pattern, file, keepSymbol);
                } catch(MemberNotFoundException x) {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Cannot resolve member '" + memberName + "' of '" + (parent != null ? parent.getClass().getSimpleName() : "null") + "'", pattern));
                    throw new EntryParsingException();
                }
            }
            case "INDEXED_MEMBER": {
                Object parent = parse(pattern.find("INTERPOLATION_VALUE"), file);
                EObject.assertNotNull(parent, pattern.find("INTERPOLATION_VALUE"), file);
                VariableTypeHandler handler = getHandlerForObject(parent, pattern, file);
                Object index = parse(pattern.find("INDEX.INTERPOLATION_VALUE"), file);
                try {
                    return handler.getIndexer(parent, index, pattern, file, keepSymbol);
                } catch(MemberNotFoundException x) {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Cannot resolve member for index " + index + " of '" + (parent != null ? parent.getClass().getSimpleName() : "null") + "'", pattern));
                    throw new EntryParsingException();
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

                    return ((VariableMethod) parent).call(params.toArray(), patterns.toArray(new TokenPattern<?>[0]), pattern, file);
                } else {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "This is not a method", pattern.find("INTERPOLATION_VALUE")));
                    throw new EntryParsingException();
                }
            }
            case "PARENTHESIZED_VALUE": {
                return parse(pattern.find("INTERPOLATION_VALUE"), file, keepSymbol);
            }
            case "CAST": {
                Object parent = parse(pattern.find("CLOSED_INTERPOLATION_VALUE"), file);
                VariableTypeHandler handler = getHandlerForObject(parent, pattern, file);
                Class newType = VariableTypeHandler.Static.getClassForShorthand(pattern.find("TARGET_TYPE").flatten(false));
                try {
                    return handler.cast(parent, newType, pattern, file);
                } catch(ClassCastException x) {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unable to cast " + (parent != null ? parent.getClass().getSimpleName() : "null") + " to type " + newType.getName(), pattern));
                    throw new EntryParsingException();
                }
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
                            Operator operator = Operator.getOperatorForSymbol(((TokenItem) contents.get(i)).getContents().value);
                            if(operator.getLeftOperandType() == OperandType.VARIABLE) {
                                flatValues.remove(flatValues.size()-1);

                                Object symbol = parse(contents.get(i-1), file, true);
                                if(symbol instanceof Symbol) {
                                    flatValues.add(symbol);
                                } else {
                                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Expected symbol in the left hand side of the expression", pattern));
                                    throw new EntryParsingException();
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
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
            }
        }
    }

    @NotNull
    public static VariableTypeHandler getHandlerForObject(Object value, TokenPattern<?> pattern, TridentFile file) {
        if(value instanceof VariableTypeHandler) return ((VariableTypeHandler) value);
        if(value == null) return new NullType();
        VariableTypeHandler handler = ParserManager.getParser(VariableTypeHandler.class, VariableTypeHandler.Static.getIdentifierForClass(value.getClass()));
        if(handler == null) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Couldn't find handler for type " + value.getClass().getName(), pattern));
            throw new EntryParsingException();
        }
        return handler;
    }
}
