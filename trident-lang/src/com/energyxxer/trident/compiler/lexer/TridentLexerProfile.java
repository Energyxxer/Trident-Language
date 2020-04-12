package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.enxlex.lexical_analysis.profiles.*;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.Lazy;
import com.energyxxer.util.StringLocation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

public class TridentLexerProfile extends LexerProfile {

    public static final Lazy<TridentLexerProfile> INSTANCE = new Lazy<>(TridentLexerProfile::new);

    public static final HashMap<TokenType, LexerContext> usefulContexts = new HashMap<>();
    private static final List<String> reservedWords = Arrays.asList("int", "real", "boolean", "string", "entity", "block", "item", "text_component", "nbt", "nbt_value", "tag_byte", "tag_short", "tag_int", "tag_float", "tag_double", "tag_long", "tag_string", "tag_compound", "tag_list", "tag_int_array", "tag_byte_array", "tag_long_array", "nbt_path", "coordinate", "resource", "int_range", "real_range", "var", "eval", "define", "do", "while", "within", "for", "switch", "function", "if", "else", "try", "catch", "new", "throw", "return", "break", "continue", "private", "local", "global", "case", "switch", "default", "component", "implements", "pointer", "true", "false");

    public static final Pattern IDENTIFIER_A_REGEX = Pattern.compile("[a-zA-Z0-9._\\-+]+");
    public static final Pattern IDENTIFIER_B_REGEX = Pattern.compile("[^@\\s]\\S*");
    public static final Pattern IDENTIFIER_B_TOKEN_REGEX = Pattern.compile("[^@$\\s][^\\s]*");
    public static final Pattern IDENTIFIER_B_LIMITED_TOKEN_REGEX = Pattern.compile("[^@$\\s]((?!->)[^\\s~>])*");
    public static final String IDENTIFIER_C_REGEX = "\\S+";
    public static final String IDENTIFIER_D_REGEX = "[a-zA-Z0-9_\\-+]+";

    public static final Pattern NUMBER_REGEX = Pattern.compile("([+-]?(?:\\d*(\\.\\d+)|\\d+))([bdfsL]?)", Pattern.CASE_INSENSITIVE);
    public static final Pattern SHORT_NUMBER_REGEX = Pattern.compile("[+-]?\\d*(\\.\\d+)?", Pattern.CASE_INSENSITIVE);
    public static final Pattern TIME_REGEX = Pattern.compile("(\\d*(\\.\\d+)|\\d+)[tsd]?");

    static {
        usefulContexts.put(RESOURCE_LOCATION, new ResourceLocationContext(Namespace.ALLOWED_NAMESPACE_REGEX.replace("+",""), Function.ALLOWED_PATH_REGEX.replace("+",""), RESOURCE_LOCATION));
    }

    public TridentLexerProfile() {
        this.initialize();
    }

    public static boolean isValidIdentifier(String str) {
        return str.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    public static boolean isReservedKeyword(String str) {
        return reservedWords.contains(str);
    }

    private void initialize() {

        //Numbers
        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                Matcher matcher = NUMBER_REGEX.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    if(length <= 0) return new ScannerContextResponse(false);
                    return new ScannerContextResponse(true, str.substring(0,length), (Character.isLetter(str.charAt(length-1)) ? TridentTokens.TYPED_NUMBER : ((str.substring(0, length).contains(".")) ? TridentTokens.REAL_NUMBER : TridentTokens.INTEGER_NUMBER)));
                } else return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                Matcher matcher = NUMBER_REGEX.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    if(length <= 0) return new ScannerContextResponse(false);

                    TokenType obtainedType = Character.isLetter(str.charAt(length-1)) ? TridentTokens.TYPED_NUMBER : ((str.substring(0, length).contains(".")) ? TridentTokens.REAL_NUMBER : TridentTokens.INTEGER_NUMBER);

                    if(type == TYPED_NUMBER) obtainedType = type;
                    else if(type == REAL_NUMBER && obtainedType == INTEGER_NUMBER) obtainedType = REAL_NUMBER;

                    if(type == obtainedType) {
                        return new ScannerContextResponse(true, str.substring(0,length), type);
                    } else return new ScannerContextResponse(false);
                } else return new ScannerContextResponse(false);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(TridentTokens.TYPED_NUMBER, TridentTokens.INTEGER_NUMBER, TridentTokens.REAL_NUMBER);
            }
        });

        //Short numbers ('.0', '.5' ...)
        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                Matcher matcher = SHORT_NUMBER_REGEX.matcher(str);

                if(matcher.lookingAt() && matcher.end() > 0) {
                    int length = matcher.end();
                    return new ScannerContextResponse(true, str.substring(0,length), SHORT_REAL_NUMBER);
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singleton(SHORT_REAL_NUMBER);
            }
        });

        //Time literal
        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                Matcher matcher = TIME_REGEX.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    return new ScannerContextResponse(true, str.substring(0,length), TridentTokens.TIME);
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(TIME);
            }
        });

        contexts.add(new StringTypeMatchLexerContext(new String[] { ".", ",", ":", "=", "(", ")", "[", "]", "{", "}", "<", ">", "~", "^", "!", "#" },
                new TokenType[] { DOT, COMMA, COLON, EQUALS, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, TILDE, CARET, NOT, HASH }
                ));

        contexts.add(new StringMatchLexerContext(TridentTokens.SCOREBOARD_OPERATOR, "%=", "*=", "+=", "-=", "/=", "><", "=", ">", "<"));

        //Glue
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                if(str.length() > 0 && Character.isWhitespace(str.charAt(0))) return new ScannerContextResponse(false);
                return new ScannerContextResponse(true, "", TridentTokens.GLUE);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(GLUE);
            }

            @Override
            public boolean ignoreLeadingWhitespace() {
                return false;
            }
        });

        //Line glue
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                for(int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if(c == '\n') return new ScannerContextResponse(false);
                    if(!Character.isWhitespace(c)) return new ScannerContextResponse(true, "", TridentTokens.LINE_GLUE);
                }
                return new ScannerContextResponse(true, "", TridentTokens.LINE_GLUE);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(LINE_GLUE);
            }

            @Override
            public boolean ignoreLeadingWhitespace() {
                return false;
            }
        });

        //Verbatim commands
        contexts.add(new LexerContext() {

            String header = "/";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                if(str.length() <= 0) return new ScannerContextResponse(false);

                if(type == VERBATIM_COMMAND_HEADER) {
                    if(str.startsWith("/")) return new ScannerContextResponse(true, "/", VERBATIM_COMMAND_HEADER);
                    else return new ScannerContextResponse(false);
                } else {
                    if(str.startsWith("$")) return new ScannerContextResponse(false);
                    int endIndex = str.length();
                    if(str.contains("\n")) {
                        endIndex = str.indexOf("\n");
                    }

                    return new ScannerContextResponse(true, str.substring(0, endIndex), VERBATIM_COMMAND);
                }
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(VERBATIM_COMMAND, VERBATIM_COMMAND_HEADER);
            }
        });

        //Trailing string
        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false );
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), TRAILING_STRING);
                } else return new ScannerContextResponse(true, str, TRAILING_STRING);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(TRAILING_STRING);
            }
        });

        //Say string
        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                int endIndex = indexOf(str, "\n", "@p", "@e", "@a", "@r", "@s");
                if(endIndex == 0) return new ScannerContextResponse(false);
                if(endIndex == -1) return new ScannerContextResponse(true, str, SAY_STRING);
                return new ScannerContextResponse(true, str.substring(0, endIndex), SAY_STRING);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(SAY_STRING);
            }

            @Override
            public boolean ignoreLeadingWhitespace() {
                return false;
            }
        });

        //Whitespace
        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                if(!str.isEmpty() && str.charAt(0) == ' ') return new ScannerContextResponse(true, " ", WHITESPACE);
                return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(WHITESPACE);
            }

            @Override
            public boolean ignoreLeadingWhitespace() {
                return false;
            }
        });

        //String literals
        contexts.add(new LexerContext() {

            String delimiters = "\"'";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(str.length() <= 0) return new ScannerContextResponse(false);
                char startingCharacter = str.charAt(0);

                if(delimiters.contains(Character.toString(startingCharacter))) {

                    String errorMessage = null;

                    StringBuilder token = new StringBuilder(Character.toString(startingCharacter));
                    StringLocation end = new StringLocation(1,0,1);

                    HashMap<TokenSection, String> escapedChars = new HashMap<>();

                    for(int i = 1; i < str.length(); i++) {
                        char c = str.charAt(i);

                        if(c == '\n') {
                            end.line++;
                            end.column = 0;
                        } else {
                            end.column++;
                        }
                        end.index++;

                        if(c == '\n') {
                            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, TridentTokens.STRING_LITERAL, escapedChars);
                            response.setError("Illegal line end in string literal", i, 1);
                            return response;
                        }
                        token.append(c);
                        if(c == '\\') {
                            if(i >= str.length()-1) {
                                break; //Unexpected end of input
                            }
                            char escapedChar = str.charAt(i+1);
                            if(!"bfnrtu\\\"'".contains(escapedChar+"")) {
                                errorMessage = "Illegal escape character in string literal";
                            } else {
                                if(escapedChar == 'u') {
                                    if(str.length() - i+2 < 4 || !str.substring(i+2, i+2+4).matches("[0-9A-Fa-f]{4}")) {
                                        errorMessage = "Illegal escape character in string literal";
                                    } else {
                                        escapedChars.put(new TokenSection(i,6), "string_literal.escape");
                                    }
                                } else {
                                    escapedChars.put(new TokenSection(i,2), "string_literal.escape");
                                }
                            }
                            token.append(escapedChar);
                            i++;
                        } else if(c == startingCharacter) {
                            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, TridentTokens.STRING_LITERAL, escapedChars);
                            if(errorMessage != null) {
                                response.setError(errorMessage, 0, i+1);
                            }
                            return response;
                        }
                    }
                    //Unexpected end of input
                    ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, TridentTokens.STRING_LITERAL, escapedChars);
                    response.setError("Unexpected end of input", str.length()-1, 1);
                    return response;
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(STRING_LITERAL);
            }
        });

        //Selector headers
        contexts.add(new LexerContext() {

            private String headers = "pears";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(str.length() < 2) return new ScannerContextResponse(false);
                if(!str.startsWith("@")) return new ScannerContextResponse(false);
                if(headers.contains(str.charAt(1) + "")) {
                    return new ScannerContextResponse(true, str.substring(0,2), SELECTOR_HEADER);
                }
                return new ScannerContextResponse(false);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(SELECTOR_HEADER);
            }
        });

        //Resource Locations
        contexts.add(usefulContexts.get(RESOURCE_LOCATION));

        //Comments
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(!str.startsWith("#")) return new ScannerContextResponse(false);
                if(str.contains("\n")) {
                    return handleComment(str.substring(0, str.indexOf("\n")));
                } else return handleComment(str);
            }

            private ScannerContextResponse handleComment(String str) {
                HashMap<TokenSection, String> sections = new HashMap<>();
                int todoIndex = str.toUpperCase(Locale.ENGLISH).indexOf("TODO");
                if(todoIndex >= 0) {
                    int todoEnd = str.indexOf("\n");
                    if(todoEnd < 0) todoEnd = str.length();
                    sections.put(new TokenSection(todoIndex, todoEnd-todoIndex), "comment.todo");
                }
                return new ScannerContextResponse(true, str, COMMENT, sections);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LINE_START;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(COMMENT);
            }
        });

        //Directive headers
        contexts.add(new StringMatchLexerContext(DIRECTIVE_HEADER, "@").setOnlyWhenExpected(true));

        //Swizzle
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            private String extractIdentifierA(String str) {
                int i = 0;
                while (i < str.length() && Character.toString(str.charAt(i)).matches("[a-zA-Z0-9._\\-+]")) {
                    i++;
                }
                return str.substring(0, i);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                str = extractIdentifierA(str);
                if(str.length() == 0 || str.length() > 3) return new ScannerContextResponse(false);

                ArrayList<Character> possibleChars = new ArrayList<>();
                possibleChars.add('x');
                possibleChars.add('y');
                possibleChars.add('z');
                for(int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if(possibleChars.contains(c)) {
                        possibleChars.remove((Character)c);
                    } else {
                        return new ScannerContextResponse(false);
                    }
                }

                return new ScannerContextResponse(true, str, SWIZZLE);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(SWIZZLE);
            }
        });

        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_A, "[a-zA-Z0-9._\\-+]"));

        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                Matcher matcher = IDENTIFIER_B_TOKEN_REGEX.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    if(length <= 0) return new ScannerContextResponse(false);
                    return new ScannerContextResponse(true, str.substring(0,length), IDENTIFIER_TYPE_B);
                } else return new ScannerContextResponse(false);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(IDENTIFIER_TYPE_B);
            }
        });

        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                Matcher matcher = IDENTIFIER_B_LIMITED_TOKEN_REGEX.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    if(length <= 0) return new ScannerContextResponse(false);
                    return new ScannerContextResponse(true, str.substring(0,length), IDENTIFIER_TYPE_B_LIMITED);
                } else return new ScannerContextResponse(false);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(IDENTIFIER_TYPE_B_LIMITED);
            }
        });




        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_C, "\\S"));
        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_D, "[^\\s\\[\\].{}\"<>]"));

        //contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_X, "[a-zA-Z0-9._]", "[a-zA-Z._]"));

        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                int i = 0;
                while(i < str.length() && (
                        (i == 0 && str.substring(i,i+1).matches("[a-zA-Z_]")
                                ||
                                (i > 0 && str.substring(i,i+1).matches("[a-zA-Z0-9_]"))
                        ))) {
                    i++;
                }
                str = str.substring(0, i);
                if(i > 0 && !reservedWords.contains(str)) return new ScannerContextResponse(true, str, type);
                return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(IDENTIFIER_TYPE_X);
            }
        });

        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                int i = 0;
                while(i < str.length() && (
                        (i == 0 && str.substring(i,i+1).matches("[a-zA-Z_]")
                                ||
                                (i > 0 && str.substring(i,i+1).matches("[a-zA-Z0-9_]"))
                        ))) {
                    i++;
                }
                str = str.substring(0, i);
                if(i > 0 && reservedWords.contains(str)) return new ScannerContextResponse(true, str, type);
                return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(PRIMITIVE_TYPE);
            }
        });

        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                int i = 0;
                while(i < str.length() && (
                        (i == 0 && str.substring(i,i+1).matches("[a-zA-Z_]")
                                ||
                                (i > 0 && str.substring(i,i+1).matches("[a-zA-Z0-9_]"))
                        ))) {
                    i++;
                }
                str = str.substring(0, i);
                if(i > 0) return new ScannerContextResponse(true, str, type);
                return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(IDENTIFIER_TYPE_Y);
            }
        });

        contexts.add(new StringMatchLexerContext(KEYWORD, "var", "define", "do", "while", "within", "using", "eval", "as", "append", "for", "in", "switch", "function", "if", "else", "try", "catch", "throw", "tdndebug", "switch", "case", "default", "implements", "log", "break", "return", "continue"));
        contexts.add(new StringMatchLexerContext(CUSTOM_COMMAND_KEYWORD, "isset", "update"));
        contexts.add(new StringMatchLexerContext(BOOLEAN, "true", "false"));
        contexts.add(new IdentifierLexerContext(COMMAND_HEADER, "[a-zA-Z0-9._\\-+]"));
        contexts.add(new IdentifierLexerContext(MODIFIER_HEADER, "[a-zA-Z0-9._\\-+]"));

        contexts.add(new StringMatchLexerContext(SYMBOL, "*", "<=", ">=", "<", ">", "!=", "=", "$", ";", "?"));
        contexts.add(new StringMatchLexerContext(ARROW, "->"));

        contexts.add(new StringMatchLexerContext(COMPILER_OPERATOR, "+=", "-=", "*=", "/=", "%=", "+", "-", "*", "/", "%", "<=", ">=", "<", ">", "==", "!=", "=", "&&", "||", "&", "|", "^"));
        contexts.add(new StringMatchLexerContext(COMPILER_POSTFIX_OPERATOR, "++", "--"));
        contexts.add(new StringMatchLexerContext(COMPILER_PREFIX_OPERATOR, "++", "--", "+", "-", "~", "!"));

        contexts.add(new StringMatchLexerContext(NULL, "null"));

        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(NO_TOKEN);
            }

            @Override
            public boolean ignoreLeadingWhitespace() {
                return false;
            }
        });

        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                return new ScannerContextResponse(true, "", EMPTY_TOKEN);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(EMPTY_TOKEN);
            }

            @Override
            public boolean ignoreLeadingWhitespace() {
                return false;
            }
        });
    }

    @Override
    public boolean canMerge(char ch0, char ch1) {
        return isValidIdentifierPart(ch0) && isValidIdentifierPart(ch1);
    }

    private boolean isValidIdentifierPart(char ch) {
        return ch != '$' && Character.isJavaIdentifierPart(ch);
    }

    @Override
    public boolean useNewlineTokens() {
        return true;
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","tdn");
        header.attributes.put("DESC","Trident Function File");
    }

    private static int indexOf(String base, String... substrings) {
        int minIndex = -1;
        for(String str : substrings) {
            int index = base.indexOf(str);
            if(minIndex == -1 || (index != -1 && index < minIndex)) {
                minIndex = index;
            }
        }
        return minIndex;
    }

    static class ResourceLocationContext implements LexerContext {
        private final String acceptedNamespaceChars;
        private final String acceptedPathChars;

        private final TokenType tokenType;

        public ResourceLocationContext(String acceptedNamespaceChars, String acceptedPathChars, TokenType tokenType) {
            this.acceptedNamespaceChars = acceptedNamespaceChars;
            this.acceptedPathChars = acceptedPathChars;

            this.tokenType = tokenType;
        }

        @Override
        public ScannerContextResponse analyze(String str, LexerProfile profile) {
            return new ScannerContextResponse(false);
        }

        @Override
        public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
            boolean namespaceFound = false;
            int nonNamespaceCharIndex = -1;
            int index = 0;
            for(char c : str.toCharArray()) {
                boolean validNs = (""+c).matches(acceptedNamespaceChars);
                boolean validPt = (""+c).matches(acceptedPathChars);
                if(!validNs && !validPt) {
                    if(!namespaceFound && c == ':') {
                        namespaceFound = true;
                        if(nonNamespaceCharIndex >= 0) break;
                    } else break;
                } else {
                    if(!namespaceFound && !validNs) {
                        if(nonNamespaceCharIndex <= 0) nonNamespaceCharIndex = index;
                    } else if(namespaceFound && !validPt) {
                        break;
                    }
                }
                if(Character.isWhitespace(c)) break;
                index++;
            }
            if(namespaceFound && nonNamespaceCharIndex >= 0) index = nonNamespaceCharIndex;
            if(index == 0) return new ScannerContextResponse(false);
            else {
                HashMap<TokenSection, String> tokenSections = new HashMap<>();
                boolean relative = str.startsWith("/");
                if(relative) {
                    tokenSections.put(new TokenSection(0, 1), "resource_location.relative");
                }

                return new ScannerContextResponse(true, str.substring(0, index), tokenType, tokenSections);
            }
        }

        @Override
        public Collection<TokenType> getHandledTypes() {
            return Collections.singletonList(tokenType);
        }
    }
}
