package com.energyxxer.trident.compiler.plugin;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.lexer.TridentTokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.NO_TOKEN;

public class TDNMetaBuilder {

    public static final String PLUGIN_CREATED_COMMAND_TAG = "__PLUGIN_CREATED";
    public static final String PLUGIN_CREATED_TAG = "__PLUGIN_CREATED";
    public static final String STORE_VAR_TAG_PREFIX = "__PLUGIN_STORE_VAR:";
    public static final String STORE_FLAT_TAG = "__PLUGIN_STORE_FLAT:";
    public static final String STORE_METADATA_TAG_PREFIX = "__PLUGIN_STORE_VAR_METADATA:";

    private static final ArrayList<FunctionDefinition> FUNCTIONS = new ArrayList<>();

    private TokenPattern<?> filePattern;
    private TridentProductions tdnProductions;

    private HashMap<String, Value> variables = new HashMap<>();

    private LazyTokenPatternMatch returnValue = null;

    public TDNMetaBuilder(TokenPattern<?> filePattern, TridentProductions tdnProductions) {
        this.filePattern = filePattern;
        this.tdnProductions = tdnProductions;
    }

    public void build() {
        variables.clear();

        TokenList statementList = ((TokenList) filePattern.find("STATEMENT_LIST"));

        if(statementList != null) {
            for(TokenPattern<?> statement : statementList.getContents()) {
                runStatement(statement);
            }
        }

        runStatement(filePattern.find("RETURN_STATEMENT"));
    }

    private void runStatement(TokenPattern<?> pattern) {
        while(true) {
            switch(pattern.getName()) {
                case "STATEMENT": {
                    pattern = ((TokenStructure) pattern).getContents();
                    continue;
                }
                case "DEFINE_STATEMENT": {
                    String definitionName = pattern.find("DEFINITION_NAME").flatten(false);
                    Value value = parseValue(pattern.find("VALUE"));
                    variables.put(definitionName, value);
                    return;
                }
                case "RETURN_STATEMENT": {
                    Value value = parseValue(pattern.find("VALUE"));
                    if(value instanceof TokenMatchValue) {
                        returnValue = ((TokenMatchValue) value).patternMatch;
                    } else {
                        throw new TDNMetaException("Return value must be a token match", pattern);
                    }
                    return;
                }
            }
        }
    }

    private Value parseValue(TokenPattern<?> pattern) {
        while(true) {
            switch(pattern.getName()) {
                case "VALUE":
                case "ROOT_VALUE": {
                    pattern = ((TokenStructure) pattern).getContents();
                    continue;
                }
                case "ROOT_IDENTIFIER": {
                    String identifier = pattern.flatten(false);
                    Value defined = variables.get(identifier);
                    if(defined != null) return defined;
                    LazyTokenPatternMatch production = tdnProductions.getStructureByName(identifier);
                    if(production != null) return new TokenMatchValue(production);
                    throw new TDNMetaException("Identifier not found: '" + identifier + "'", pattern);
                }
                case "ROOT_STRING_LITERAL": {
                    String raw = pattern.flatten(false);
                    return new StringLiteralValue(CommandUtils.parseQuotedString(raw));
                }
                case "ROOT_BOOLEAN": {
                    return new BooleanValue(pattern.flatten(false).equals("true"));
                }
                case "ROOT_FUNCTION": {
                    String identifier = pattern.flatten(false);
                    Optional<FunctionDefinition> defined = FUNCTIONS.stream().filter(f -> f.functionName.equals(identifier) && f.memberOfType == null).findFirst();
                    if(defined.isPresent()) return defined.get().createForValue(null);
                    throw new TDNMetaException("Function not found: '" + identifier + "'", pattern);
                }
                case "MEMBER_ACCESS_VALUE": {
                    Value value = parseValue(pattern.find("ROOT_VALUE"));

                    TokenList memberAccessList = ((TokenList) pattern.find("MEMBER_ACCESS_LIST"));
                    if(memberAccessList != null) {
                        for(TokenPattern<?> memberAccess : memberAccessList.getContents()) {
                            value = resolveMemberAccess(value, memberAccess);
                        }
                    }

                    return value;
                }
            }
        }
    }

    private Value resolveMemberAccess(Value value, TokenPattern<?> pattern) {
        while(true) {
            switch(pattern.getName()) {
                case "MEMBER_ACCESS": {
                    pattern = ((TokenStructure) pattern).getContents();
                    continue;
                }
                case "FUNCTION_MEMBER": {
                    String functionName = pattern.find("FUNCTION_NAME").flatten(false);
                    Optional<FunctionDefinition> defined = FUNCTIONS.stream().filter(f -> f.functionName.equals(functionName) && f.memberOfType == value.getClass()).findFirst();
                    if(defined.isPresent()) return defined.get().createForValue(value);
                    throw new TDNMetaException("Function not found: '" + functionName + "'; Not defined as member of " + value.getClass() + " type", pattern);
                }
                case "FUNCTION_CALL": {
                    if(value instanceof FunctionValue) {
                        ArrayList<Value> args = new ArrayList<>();
                        TokenList argList = ((TokenList) pattern.find("ARGUMENT_LIST"));
                        if(argList != null) {
                            for(TokenPattern<?> rawArg : argList.searchByName("VALUE")) {
                                args.add(parseValue(rawArg));
                            }
                        }
                        return ((FunctionValue) value).evaluate(args);
                    } else {
                        throw new TDNMetaException("Not a function", pattern);
                    }
                }
            }
        }
    }

    public LazyTokenPatternMatch getReturnValue() {
        return returnValue;
    }

    private static abstract class Value {
    }

    private static class TokenMatchValue extends Value {
        public LazyTokenPatternMatch patternMatch;

        public TokenMatchValue(LazyTokenPatternMatch patternMatch) {
            this.patternMatch = patternMatch;
        }
    }

    private static class FunctionDefinition {
        public final String functionName;
        public final Class<? extends Value> memberOfType;
        public final BiFunction<Value, List<Value>, Value> handler;

        public FunctionDefinition(String functionName, Class<? extends Value> memberOfType, BiFunction<Value, List<Value>, Value> handler) {
            this.functionName = functionName;
            this.memberOfType = memberOfType;
            this.handler = handler;
        }

        public FunctionValue createForValue(Value value) {
            return new FunctionValue(value, this);
        }
    }

    private static class FunctionValue extends Value {
        public final Value functionOwner;
        public final FunctionDefinition definition;

        public FunctionValue(Value functionOwner, FunctionDefinition definition) {
            this.functionOwner = functionOwner;
            this.definition = definition;
        }

        public Value evaluate(List<Value> args) {
            return definition.handler.apply(functionOwner, args);
        }
    }

    private static class StringLiteralValue extends Value {
        public String stringValue;

        public StringLiteralValue(String stringValue) {
            this.stringValue = stringValue;
        }
    }

    private static class BooleanValue extends Value {
        public boolean boolValue;

        public BooleanValue(boolean boolValue) {
            this.boolValue = boolValue;
        }
    }

    private static void registerFunction(String functionName, BiFunction<Value, List<Value>, Value> handler) {
        registerFunction(functionName, null, handler);
    }

    private static void registerFunction(String functionName, Class<? extends Value> memberOfType, BiFunction<Value, List<Value>, Value> handler) {
        FUNCTIONS.add(new FunctionDefinition(functionName, memberOfType, handler));
    }

    public static class TDNMetaException extends RuntimeException {
        private String error;
        private TokenPattern<?> cause;

        public TDNMetaException(String error, TokenPattern<?> cause) {
            this.error = error;
            this.cause = cause;
        }

        public String getErrorMessage() {
            return error;
        }

        public TokenPattern<?> getCausedBy() {
            return cause;
        }
    }

    static {
        registerFunction("noToken", (ignore, args) -> {
            LazyTokenItemMatch nt = new LazyTokenItemMatch(NO_TOKEN);
            nt.addTags(PLUGIN_CREATED_TAG);
            nt.setOptional();
            return new TokenMatchValue(nt);
        });
        registerFunction("group", (ignore, args) -> {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.addTags(PLUGIN_CREATED_TAG);
            for(Value arg : args) {
                if(arg instanceof TokenMatchValue) {
                    g.append(((TokenMatchValue) arg).patternMatch);
                } else throw new IllegalArgumentException("Function 'group' only accepts Token Match values, found " + arg.getClass().getSimpleName());
            }
            return new TokenMatchValue(g);
        });
        registerFunction("optional", (ignore, args) -> {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch(true);
            g.addTags(PLUGIN_CREATED_TAG);
            for(Value arg : args) {
                if(arg instanceof TokenMatchValue) {
                    g.append(((TokenMatchValue) arg).patternMatch);
                } else throw new IllegalArgumentException("Function 'optional' only accepts Token Match values, found " + arg.getClass().getSimpleName());
            }
            return new TokenMatchValue(g);
        });
        registerFunction("choice", (ignore, args) -> {
            LazyTokenStructureMatch s = new LazyTokenStructureMatch("CHOICE");
            s.addTags(PLUGIN_CREATED_TAG);
            if(args.size() == 0) {
                throw new IllegalArgumentException("Function 'choice' requires at least 1 parameter, found " + args.size());
            }
            for(Value arg : args) {
                if(arg instanceof TokenMatchValue) {
                    s.add(((TokenMatchValue) arg).patternMatch);
                } else if(arg instanceof StringLiteralValue) {
                    String text = ((StringLiteralValue) arg).stringValue;
                    s.add(new LazyTokenItemMatch(TokenType.UNKNOWN, text).setName("LITERAL_" + text.toUpperCase()).addTags(SuggestionTags.ENABLED, PLUGIN_CREATED_TAG));
                } else {
                    throw new IllegalArgumentException("Function 'choice' only accepts Token Match values or Strings, found " + arg.getClass().getSimpleName());
                }
            }
            return new TokenMatchValue(s);
        });
        registerFunction("list", (ignore, args) -> {
            LazyTokenPatternMatch listValue;
            LazyTokenPatternMatch separatorValue = null;
            if(args.size() >= 1) {
                if(args.get(0) instanceof TokenMatchValue) {
                    listValue = ((TokenMatchValue) args.get(0)).patternMatch;
                } else {
                    throw new IllegalArgumentException("Function 'list' only accepts Token Match values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'list' requires at least 1 parameter, found " + args.size());
            }
            if(args.size() >= 2) {
                if(args.get(1) instanceof TokenMatchValue) {
                    separatorValue = ((TokenMatchValue) args.get(1)).patternMatch;
                } else {
                    throw new IllegalArgumentException("Function 'list' only accepts Token Match values at argument 1, found " + args.get(1).getClass().getSimpleName());
                }
            }
            return new TokenMatchValue(new LazyTokenListMatch(listValue, separatorValue).addTags(PLUGIN_CREATED_TAG));
        });
        registerFunction("name", TokenMatchValue.class, (value, args) -> {
            if(args.size() >= 1) {
                if(args.get(0) instanceof StringLiteralValue) {
                    if(((TokenMatchValue) value).patternMatch.tags.contains(PLUGIN_CREATED_TAG)) {
                        ((TokenMatchValue) value).patternMatch.setName(((StringLiteralValue) args.get(0)).stringValue);
                    } else {
                        throw new IllegalArgumentException("Function 'name' can only be performed on plugin-created patterns");
                    }
                } else {
                    throw new IllegalArgumentException("Function 'name' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'name' requires at least 1 parameter, found " + args.size());
            }
            return value;
        });
        registerFunction("hint", TokenMatchValue.class, (value, args) -> {
            if(args.size() >= 1) {
                if(args.get(0) instanceof StringLiteralValue) {
                    if(((TokenMatchValue) value).patternMatch.tags.contains(PLUGIN_CREATED_TAG)) {
                        ((TokenMatchValue) value).patternMatch.addTags("cspn:" + ((StringLiteralValue) args.get(0)).stringValue);
                    } else {
                        throw new IllegalArgumentException("Function 'hint' can only be performed on plugin-created patterns");
                    }
                } else {
                    throw new IllegalArgumentException("Function 'hint' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'hint' requires at least 1 parameter, found " + args.size());
            }
            return value;
        });
        registerFunction("storeVar", TokenMatchValue.class, (value, args) -> {
            if(args.size() >= 1) {
                if(args.get(0) instanceof StringLiteralValue) {
                    if(!((TokenMatchValue) value).patternMatch.tags.contains(PLUGIN_CREATED_TAG)) {
                        LazyTokenGroupMatch match = new LazyTokenGroupMatch().append(((TokenMatchValue) value).patternMatch);
                        match.addTags(PLUGIN_CREATED_TAG).addTags(STORE_VAR_TAG_PREFIX + ((StringLiteralValue) args.get(0)).stringValue);
                        for(int i = 1; i < args.size(); i++) {
                            Value arg = args.get(i);
                            if(arg instanceof StringLiteralValue) {
                                match.addTags(STORE_METADATA_TAG_PREFIX + ((StringLiteralValue) arg).stringValue);
                            } else {
                                throw new IllegalArgumentException("Function 'storeVar' only accepts String Literal values at arguments 1 and later, found " + arg.getClass().getSimpleName());
                            }
                        }
                        return new TokenMatchValue(match);
                    } else {
                        throw new IllegalArgumentException("Function 'storeVar' can only be performed on native patterns");
                    }
                } else {
                    throw new IllegalArgumentException("Function 'storeVar' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'storeVar' requires at least 1 parameter, found " + args.size());
            }
        });
        registerFunction("storeFlat", TokenMatchValue.class, (value, args) -> {
            if(args.size() >= 1) {
                if(args.get(0) instanceof StringLiteralValue) {
                    return new TokenMatchValue(new LazyTokenGroupMatch().append(((TokenMatchValue) value).patternMatch).addTags(PLUGIN_CREATED_TAG).addTags(STORE_FLAT_TAG).addTags(STORE_VAR_TAG_PREFIX + ((StringLiteralValue) args.get(0)).stringValue));
                } else {
                    throw new IllegalArgumentException("Function 'storeVar' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'storeVar' requires at least 1 parameter, found " + args.size());
            }
        });
        registerFunction("optional", TokenMatchValue.class, (value, args) -> {
            boolean shouldBeOptional = true;
            if(args.size() >= 1) {
                if(args.get(0) instanceof BooleanValue) {
                    shouldBeOptional = ((BooleanValue) args.get(0)).boolValue;
                } else {
                    throw new IllegalArgumentException("Function 'optional' only accepts Boolean values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            }
            if(((TokenMatchValue) value).patternMatch.tags.contains(PLUGIN_CREATED_TAG)) {
                ((TokenMatchValue) value).patternMatch.setOptional(shouldBeOptional);
            } else {
                throw new IllegalArgumentException("Function 'optional' can only be performed on plugin-created patterns");
            }
            return value;
        });
        registerFunction("literal", (ignore, args) -> {
            if(args.size() >= 1) {
                if(args.get(0) instanceof StringLiteralValue) {
                    String text = ((StringLiteralValue) args.get(0)).stringValue;
                    return new TokenMatchValue(new LazyTokenItemMatch(TokenType.UNKNOWN, text).setName("LITERAL_" + text.toUpperCase()).addTags(SuggestionTags.ENABLED, PLUGIN_CREATED_TAG));
                } else {
                    throw new IllegalArgumentException("Function 'hint' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'hint' requires at least 1 parameter, found " + args.size());
            }
        });
        registerFunction("brace", (ignore, args) -> {
            if(args.size() >= 1) {
                if(args.get(0) instanceof StringLiteralValue) {
                    String text = ((StringLiteralValue) args.get(0)).stringValue;
                    return new TokenMatchValue(new LazyTokenItemMatch(TridentTokens.BRACE, text).addTags(PLUGIN_CREATED_TAG));
                } else {
                    throw new IllegalArgumentException("Function 'brace' only accepts String Literal values at argument 0, found " + args.get(0).getClass().getSimpleName());
                }
            } else {
                throw new IllegalArgumentException("Function 'brace' requires at least 1 parameter, found " + args.size());
            }
        });
    }
}
