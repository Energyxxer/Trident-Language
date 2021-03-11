package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.enxlex.lexical_analysis.profiles.*;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.Lazy;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

public class TridentLexerProfile extends LexerProfile {

    public static final Lazy<TridentLexerProfile> INSTANCE = new Lazy<>(TridentLexerProfile::new);

    public static final List<String> allPrimitiveTypes = Arrays.asList("int", "real", "boolean", "string", "entity", "block", "item", "text_component", "nbt", "nbt_value", "tag_byte", "tag_short", "tag_int", "tag_float", "tag_double", "tag_long", "tag_string", "tag_compound", "tag_list", "tag_int_array", "tag_byte_array", "tag_long_array", "nbt_path", "coordinates", "resource", "int_range", "real_range", "function", "pointer", "dictionary", "list", "exception", "custom_item", "custom_entity", "entity_event", "type_definition", "rotation", "uuid");

    public static final Pattern IDENTIFIER_A_REGEX = Pattern.compile("[a-zA-Z0-9._\\-+]+");
    public static final Pattern IDENTIFIER_B_REGEX = Pattern.compile("[^@\\s]\\S*");
    public static final Pattern IDENTIFIER_B_TOKEN_REGEX = Pattern.compile("[^@$\\s][^\\s]*");
    public static final Pattern IDENTIFIER_B_LIMITED_TOKEN_REGEX = Pattern.compile("[^@$\\s]((?!->)[^\\s~>])*");
    public static final String IDENTIFIER_C_REGEX = "\\S+";
    public static final String IDENTIFIER_D_REGEX = "[a-zA-Z0-9_\\-+]+";

    public static final Pattern NUMBER_REGEX = Pattern.compile("(?:0x[0-9a-f]+)|(?:0b[01]+)|(?:([+-]?(?:\\d*(\\.\\d+)|\\d+)(?:E[+-]\\d+)?)([bdfsL]?))", Pattern.CASE_INSENSITIVE);
    public static final Pattern SHORT_NUMBER_REGEX = Pattern.compile("[+-]?\\d*(\\.\\d+)?", Pattern.CASE_INSENSITIVE);
    public static final Pattern TIME_REGEX = Pattern.compile("(\\d*(\\.\\d+)|\\d+)[tsd]?");

    public static final Pattern UUID_REGEX = Pattern.compile("[0-9a-f]{1,8}-[0-9a-f]{1,4}-[0-9a-f]{1,4}-[0-9a-f]{1,4}-[0-9a-f]{1,12}", Pattern.CASE_INSENSITIVE);

    public static final LexerContext RESOURCE_LOCATION_CONTEXT;

    static {
        RESOURCE_LOCATION_CONTEXT = new ResourceLocationContext(Namespace.ALLOWED_NAMESPACE_REGEX.replace("+",""), Function.ALLOWED_PATH_REGEX.replace("+",""), RESOURCE_LOCATION);
    }

    public TridentLexerProfile() {
        this.initialize();
    }

    public static boolean isValidIdentifier(String str) {
        return str.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    private void initialize() {

        //Numbers
        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                Matcher matcher = NUMBER_REGEX.matcher(str).region(startIndex, str.length());

                if(matcher.lookingAt()) {
                    int length = matcher.end() - matcher.start();
                    if(length <= 0) return ScannerContextResponse.FAILED;
                    String substring = str.substring(startIndex, startIndex + length);
                    return new ScannerContextResponse(true, substring, (Character.isLetter(str.charAt(startIndex+length-1)) ? TYPED_NUMBER : ((substring.contains(".")) ? REAL_NUMBER : INTEGER_NUMBER)));
                } else return ScannerContextResponse.FAILED;
            } //substring done

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                Matcher matcher = NUMBER_REGEX.matcher(str).region(startIndex, str.length());

                if(matcher.lookingAt()) {
                    int length = matcher.end() - matcher.start();
                    if(length <= 0) return ScannerContextResponse.FAILED;

                    String substring = str.substring(startIndex, startIndex + length);
                    TokenType obtainedType = length >= 2 && Character.isLetter(str.charAt(startIndex+length-1)) && ((length == 2) == (Character.isLetter(str.charAt(startIndex+1)))) ? TYPED_NUMBER : ((substring.contains(".") || substring.contains("e") || substring.contains("E")) ? REAL_NUMBER : INTEGER_NUMBER);

                    if(type == JSON_NUMBER && obtainedType != TYPED_NUMBER) obtainedType = type;

                    if(type == TYPED_NUMBER) obtainedType = type;
                    else if(type == REAL_NUMBER && obtainedType == INTEGER_NUMBER) obtainedType = REAL_NUMBER;

                    if(type == obtainedType) {
                        return new ScannerContextResponse(true, substring, type);
                    } else return ScannerContextResponse.FAILED;
                } else return ScannerContextResponse.FAILED;
            } //substring done

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(TYPED_NUMBER, INTEGER_NUMBER, REAL_NUMBER, JSON_NUMBER);
            }
        });

        //Short numbers ('.0', '.5' ...)
        contexts.add(new RegexLexerContext(SHORT_NUMBER_REGEX, SHORT_REAL_NUMBER, false));

        //UUIDs
        contexts.add(new RegexLexerContext(UUID_REGEX, UUID, false));

        //Time literal
        contexts.add(new RegexLexerContext(TIME_REGEX, TIME, true));

        contexts.add(new StringTypeMatchLexerContext(new String[] { ".", ",", ":", "=", "(", ")", "[", "]", "{", "}", "<", ">", "~", "^", "!", "#" },
                new TokenType[] { DOT, COMMA, COLON, EQUALS, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, TILDE, CARET, NOT, HASH }
        ));

        contexts.add(new StringMatchLexerContext(SCOREBOARD_OPERATOR, "%=", "*=", "+=", "-=", "/=", "><", "=", ">", "<"));

        //Glue
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                if(str.length() - startIndex > 0 && Character.isWhitespace(str.charAt(startIndex))) return ScannerContextResponse.FAILED;
                return new ScannerContextResponse(true, "", GLUE);
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
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                for(int i = startIndex; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if(c == '\n') return ScannerContextResponse.FAILED;
                    if(!Character.isWhitespace(c)) return new ScannerContextResponse(true, "", LINE_GLUE);
                }
                return new ScannerContextResponse(true, "", LINE_GLUE);
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
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                if(str.length() - startIndex <= 0) return ScannerContextResponse.FAILED;

                if(type == VERBATIM_COMMAND_HEADER) {
                    if(str.startsWith("/", startIndex) && !str.startsWith("/>", startIndex)) return new ScannerContextResponse(true, "/", VERBATIM_COMMAND_HEADER);
                    else return ScannerContextResponse.FAILED;
                } else {
                    if(str.startsWith("$", startIndex) || str.startsWith(">", startIndex)) return ScannerContextResponse.FAILED;
                    int endIndex = str.length();
                    if(str.indexOf("\n", startIndex) != -1) {
                        endIndex = str.indexOf("\n", startIndex);
                    }

                    return new ScannerContextResponse(true, str.substring(startIndex, endIndex), VERBATIM_COMMAND);
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
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                if(str.indexOf("\n", startIndex) != -1) {
                    return new ScannerContextResponse(true, str.substring(startIndex, str.indexOf("\n", startIndex)), TRAILING_STRING);
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
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                int endIndex = startIndex;
                while(endIndex < str.length()) {
                    char c = str.charAt(endIndex);
                    if(c == '\n') break;
                    if(c == '@' && endIndex < str.length()-1 && "pears".indexOf(str.charAt(endIndex+1)) != -1) {
                        break;
                    }
                    endIndex++;
                }
                if(endIndex == startIndex) return ScannerContextResponse.FAILED;
                return new ScannerContextResponse(true, str.substring(startIndex, endIndex), SAY_STRING);
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
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                if(str.startsWith(" ", startIndex)) return new ScannerContextResponse(true, " ", WHITESPACE);
                return ScannerContextResponse.FAILED;
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
        contexts.add(new StringLiteralLexerContext("\"'", STRING_LITERAL));

        //Selector headers
        contexts.add(new LexerContext() {

            private String headers = "pears";

            @Override
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                if(str.length() - startIndex < 2) return ScannerContextResponse.FAILED;
                if(!str.startsWith("@", startIndex)) return ScannerContextResponse.FAILED;
                if(headers.contains(str.charAt(startIndex+1) + "")) {
                    return new ScannerContextResponse(true, str.substring(startIndex,startIndex+2), SELECTOR_HEADER);
                }
                return ScannerContextResponse.FAILED;
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
        contexts.add(RESOURCE_LOCATION_CONTEXT);

        //Comments
        contexts.add(new CommentLexerContext("#", COMMENT));

        //Directive headers
        contexts.add(new StringMatchLexerContext(DIRECTIVE_HEADER, "@").setOnlyWhenExpected(true));

        //Swizzle
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            private String extractIdentifierA(String str, int startIndex) {
                int i = startIndex;
                while (i < str.length() && Character.toString(str.charAt(i)).matches("[a-zA-Z0-9._\\-+]")) {
                    i++;
                }
                return str.substring(startIndex, i);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                str = extractIdentifierA(str, startIndex);
                if(str.length() == 0 || str.length() > 3) return ScannerContextResponse.FAILED;

                ArrayList<Character> possibleChars = new ArrayList<>();
                possibleChars.add('x');
                possibleChars.add('y');
                possibleChars.add('z');
                for(int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if(possibleChars.contains(c)) {
                        possibleChars.remove((Character)c);
                    } else {
                        return ScannerContextResponse.FAILED;
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

        contexts.add(new RegexLexerContext(IDENTIFIER_B_TOKEN_REGEX, IDENTIFIER_TYPE_B, true) {
            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }
        });

        contexts.add(new RegexLexerContext(IDENTIFIER_B_LIMITED_TOKEN_REGEX, IDENTIFIER_TYPE_B_LIMITED, false) {
            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }
        });


        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_C, "\\S"));
        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_D, "[^\\s\\[\\].{}\"<>]"));

        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_X, "[a-zA-Z0-9_]", "[a-zA-Z_]"));

        contexts.add(new StringMatchLexerContext(PRIMITIVE_TYPE, allPrimitiveTypes.toArray(new String[0])).setOnlyWhenExpected(true));

        contexts.add(new StringMatchLexerContext(KEYWORD, "var", "define", "do", "while", "within", "using", "eval", "as", "append", "for", "in", "switch", "function", "if", "else", "try", "catch", "throw", "tdndebug", "switch", "case", "default", "implements", "log", "break", "return", "continue", "is"));
        contexts.add(new StringMatchLexerContext(CUSTOM_COMMAND_KEYWORD, "isset", "update"));
        contexts.add(new StringMatchLexerContext(BOOLEAN, "true", "false"));
        contexts.add(new IdentifierLexerContext(COMMAND_HEADER, "[a-zA-Z0-9._\\-+:]"));
        contexts.add(new IdentifierLexerContext(MODIFIER_HEADER, "[a-zA-Z0-9._\\-+]"));

        contexts.add(new StringMatchLexerContext(SYMBOL, "*", "<=", ">=", "<", ">", "!=", "=", "$", ";", "?"));
        contexts.add(new StringMatchLexerContext(ARROW, "->"));

        contexts.add(new StringMatchLexerContext(COMPILER_OPERATOR, "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "^=", "|=", "+", "-", "*", "/", "%", "<=", ">=", "<<=", ">>=", "<<", ">>", "<", ">", "==", "!=", "=", "&&", "||", "&", "|", "^", "??", "?", ":", "~", "!"));

        contexts.add(new StringMatchLexerContext(NULL, "null"));

        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
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
            public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
                return ScannerContextResponse.FAILED;
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
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
        header.putAttribute("TYPE","tdn");
        header.putAttribute("DESC","Trident Function File");
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
        public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
            return ScannerContextResponse.FAILED;
        }

        @Override
        public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
            boolean namespaceFound = false;
            int nonNamespaceCharIndex = -1;
            int length = 0;
            for(int i = startIndex; i < str.length(); i++) {
                char c = str.charAt(i);
                boolean validNs = (""+c).matches(acceptedNamespaceChars);
                boolean validPt = (""+c).matches(acceptedPathChars);
                if(!validNs && !validPt) {
                    if(!namespaceFound && c == ':') {
                        namespaceFound = true;
                        if(nonNamespaceCharIndex >= 0) break;
                    } else break;
                } else {
                    if(!namespaceFound && !validNs) {
                        if(nonNamespaceCharIndex <= 0) nonNamespaceCharIndex = length;
                    } else if(namespaceFound && !validPt) {
                        break;
                    }
                }
                if(Character.isWhitespace(c)) break;
                length++;
            }
            if(namespaceFound && nonNamespaceCharIndex >= 0) length = nonNamespaceCharIndex;
            if(length == 0) return ScannerContextResponse.FAILED;
            else {
                HashMap<TokenSection, String> tokenSections = new HashMap<>();
                boolean relative = str.startsWith("/", startIndex);
                if(relative) {
                    tokenSections.put(new TokenSection(0, 1), "resource_location.relative");
                }

                return new ScannerContextResponse(true, str.substring(startIndex, startIndex+length), tokenType, tokenSections);
            }
        } //substring done

        @Override
        public Collection<TokenType> getHandledTypes() {
            return Collections.singletonList(tokenType);
        }
    }
}
