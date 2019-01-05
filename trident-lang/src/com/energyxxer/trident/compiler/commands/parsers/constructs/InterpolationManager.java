package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.enxlex.pattern_matching.structures.TokenItem;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.ListType;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperandType;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperationOrder;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.Operator;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperatorHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class InterpolationManager {

    @SuppressWarnings("unchecked")
    public static <T> T parse(TokenPattern<?> pattern, TridentCompiler compiler, Class<T> expected) {
        Object obj = parse(pattern, compiler, false);
        if(expected.isInstance(obj)) {
            return (T) obj;
        } else {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type " + expected.getSimpleName(), pattern));
            throw new EntryParsingException();
        }
    }

    public static Object parse(TokenPattern<?> pattern, TridentCompiler compiler, Class... expected) {
        Object obj = parse(pattern, compiler, false);
        for(Class cls : expected) {
            if(cls.isInstance(obj)) return obj;
        }
        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type " + Arrays.asList(expected).map((Class c) -> c.getSimpleName()).toSet().join(", "), pattern));
        throw new EntryParsingException();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static Object parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        return parse(pattern, compiler, false);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static Object parse(TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        switch(pattern.getName()) {
            case "INTERPOLATION_BLOCK": {
                return parse(((TokenStructure) pattern).getContents(), compiler, keepSymbol);
            }
            case "VARIABLE": {
                return parse(pattern.find("VARIABLE_NAME"), compiler, keepSymbol);
            }
            case "INTERPOLATION_WRAPPER": {
                return parse(pattern.find("INTERPOLATION_VALUE"), compiler, keepSymbol);
            }
            case "CLOSED_INTERPOLATION_VALUE": {
                return parse(((TokenStructure) pattern).getContents(), compiler, keepSymbol);
            }
            case "INTERPOLATION_VALUE": {
                return parse(((TokenStructure) pattern).getContents(), compiler, keepSymbol);
            }
            case "VARIABLE_NAME": {
                Symbol symbol = compiler.getStack().search(pattern.flatten(false));
                if(symbol == null) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + pattern.flatten(false) + "' is not defined", pattern));
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
                return EntityParser.parseEntity(pattern.find("ENTITY"), compiler);
            }
            case "WRAPPED_BLOCK": {
                return CommonParsers.parseBlock(pattern.find("BLOCK_TAGGED"), compiler);
            }
            case "WRAPPED_ITEM": {
                return CommonParsers.parseBlock(pattern.find("ITEM_TAGGED"), compiler);
            }
            case "WRAPPED_TEXT_COMPONENT": {
                return TextParser.parseTextComponent(pattern.find("TEXT_COMPONENT"), compiler);
            }
            case "WRAPPED_NBT": {
                return NBTParser.parseCompound(pattern.find("NBT_COMPOUND"), compiler);
            }
            case "WRAPPED_NBT_VALUE": {
                return NBTParser.parseValue(pattern.find("NBT_VALUE"), compiler);
            }
            case "WRAPPED_NBT_PATH": {
                return NBTParser.parsePath(pattern.find("NBT_PATH"), compiler);
            }
            case "WRAPPED_COORDINATE": {
                return CoordinateParser.parse(pattern.find("COORDINATE_SET"), compiler);
            }
            case "WRAPPED_INT_RANGE": {
                return CommonParsers.parseIntRange(pattern.find("INTEGER_NUMBER_RANGE"), compiler);
            }
            case "WRAPPED_REAL_RANGE": {
                return CommonParsers.parseRealRange(pattern.find("REAL_NUMBER_RANGE"), compiler);
            }
            case "WRAPPED_RESOURCE": {
                return CommonParsers.parseResourceLocation(pattern.find("RESOURCE_LOCATION_TAGGED"), compiler);
            }
            case "WRAPPED_DICTIONARY": {
                return parse(pattern.find("DICTIONARY"), compiler, keepSymbol);
            }
            case "DICTIONARY": {
                DictionaryObject dict = new DictionaryObject();

                TokenList entryList = (TokenList) pattern.find("DICTIONARY_ENTRY_LIST");

                if(entryList != null) {
                    for(TokenPattern<?> entry : entryList.searchByName("DICTIONARY_ENTRY")) {
                        String key = entry.find("DICTIONARY_KEY").flatten(false);
                        Object value = parse(entry.find("INTERPOLATION_VALUE"), compiler, keepSymbol);
                        dict.put(key, value);
                    }
                }

                return dict;
            }
            case "WRAPPED_LIST": {
                return parse(pattern.find("LIST"), compiler, keepSymbol);
            }
            case "LIST": {
                ListType list = new ListType();

                TokenList entryList = (TokenList) pattern.find("LIST_ENTRIES");

                if (entryList != null) {
                    for (TokenPattern<?> entry : entryList.searchByName("INTERPOLATION_VALUE")) {
                        list.add(parse(entry, compiler));
                    }

                    return list;
                }
            }
            case "MEMBER": {
                Object parent = parse(pattern.find("INTERPOLATION_VALUE"), compiler, keepSymbol);
                VariableTypeHandler handler = parent instanceof VariableTypeHandler ? (VariableTypeHandler) parent : ParserManager.getParser(VariableTypeHandler.class, VariableTypeHandler.Static.getIdentifierForClass(parent.getClass()));
                if(handler == null) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Couldn't find handler for type " + parent.getClass().getName(), pattern));
                    throw new EntryParsingException();
                }
                String memberName = pattern.find("MEMBER_NAME").flatten(false);
                Object member = handler.getMember(parent, memberName, pattern, compiler, keepSymbol);
                if(member == null) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Cannot resolve member '" + memberName + "' of '" + parent.getClass().getSimpleName() + "'", pattern));
                    throw new EntryParsingException();
                }
                return member;
            }
            case "INDEXED_MEMBER": {
                Object parent = parse(pattern.find("INTERPOLATION_VALUE"), compiler, keepSymbol);
                VariableTypeHandler handler = parent instanceof VariableTypeHandler ? (VariableTypeHandler) parent : ParserManager.getParser(VariableTypeHandler.class, VariableTypeHandler.Static.getIdentifierForClass(parent.getClass()));
                if(handler == null) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Couldn't find handler for type " + parent.getClass().getName(), pattern));
                    throw new EntryParsingException();
                }
                Object index = parse(pattern.find("INDEX.INTERPOLATION_VALUE"), compiler);
                Object member = handler.getIndexer(parent, index, pattern, compiler, keepSymbol);
                if(member == null) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Cannot resolve member for index '" + index + "' of '" + parent.getClass().getSimpleName() + "'", pattern));
                    throw new EntryParsingException();
                }
                return member;
            }
            case "METHOD_CALL": {
                Object parent = parse(pattern.find("INTERPOLATION_VALUE"), compiler, keepSymbol);
                if(parent instanceof VariableMethod) {

                    ArrayList<Object> params = new ArrayList<>();
                    ArrayList<TokenPattern<?>> patterns = new ArrayList<>();

                    TokenList paramList = (TokenList) pattern.find("PARAMETERS");

                    if(paramList != null) {
                        for(TokenPattern<?> rawParam : paramList.getContents()) {
                            if(rawParam.getName().equals("INTERPOLATION_VALUE")) {
                                params.add(parse(rawParam, compiler, keepSymbol));
                                patterns.add(rawParam);
                            }
                        }
                    }

                    return ((VariableMethod) parent).call(params.toArray(), patterns.toArray(new TokenPattern<?>[0]), pattern, compiler);
                } else {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "This is not a method", pattern.find("INTERPOLATION_VALUE")));
                    throw new EntryParsingException();
                }
            }
            case "PARENTHESIZED_VALUE": {
                return parse(pattern.find("INTERPOLATION_VALUE"), compiler, keepSymbol);
            }
            case "CAST": {
                Object parent = parse(pattern.find("CLOSED_INTERPOLATION_VALUE"), compiler, keepSymbol);
                VariableTypeHandler handler = parent instanceof VariableTypeHandler ? (VariableTypeHandler) parent : ParserManager.getParser(VariableTypeHandler.class, VariableTypeHandler.Static.getIdentifierForClass(parent.getClass()));
                if(handler == null) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Couldn't find handler for type " + parent.getClass().getName(), pattern));
                    throw new EntryParsingException();
                }
                Class newType = VariableTypeHandler.Static.getClassForShorthand(pattern.find("TARGET_TYPE").flatten(false));
                Object converted = handler.cast(parent, newType, pattern, compiler);
                if(converted == null) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unable to cast " + parent.getClass().getSimpleName() + " to type " + newType.getName(), pattern));
                    throw new EntryParsingException();
                }
                return converted;
            }
            case "EXPRESSION": {
                //region Expression evaluation
                TokenList list = (TokenList) pattern;

                if(list.size() == 1) {
                    return parse(list.getContents()[0], compiler, keepSymbol);
                } else {

                    ArrayList<TokenPattern<?>> contents = new ArrayList<>(Arrays.asList(list.getContents()));

                    ArrayList<Object> flatValues = new ArrayList<>();
                    ArrayList<Operator> flatOperators = new ArrayList<>();

                    for(int i = 0; i < contents.size(); i++) {
                        if((i & 1) == 0) {
                            //Operand
                            Object value = parse(contents.get(i), compiler, keepSymbol);
                            flatValues.add(value);
                        } else {
                            //Operator
                            Operator operator = Operator.getOperatorForSymbol(((TokenItem) contents.get(i)).getContents().value);
                            if(operator.getLeftOperandType() == OperandType.VARIABLE) {
                                flatValues.remove(flatValues.size()-1);

                                Object symbol = parse(contents.get(i-1), compiler, true);
                                if(symbol instanceof Symbol) {
                                    flatValues.add(symbol);
                                } else {
                                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Expected symbol in the left hand side of the expression", pattern));
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

                        Object result = OperatorHandler.Static.perform(a, topOperator, b, pattern, compiler);

                        flatValues.add(index, result);
                        flatOperators.remove(index);
                    }

                    return flatValues.get(0);
                }
                //endregion
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
            }
        }
    }
}
