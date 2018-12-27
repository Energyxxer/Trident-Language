package com.energyxxer.nbtmapper.parser;

import com.energyxxer.commodore.defpacks.CategoryDeclaration;
import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.enxlex.lexical_analysis.profiles.*;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringLocation;

import java.io.IOException;
import java.util.*;

import static com.energyxxer.nbtmapper.parser.NBTTMTokens.*;

public class NBTTMLexerProfile extends LexerProfile {

    /**
     * Holds the previous token for multi-token analysis.
     * */
    private Token tokenBuffer = null;

    public NBTTMLexerProfile(DefinitionPack defPack) {
        ArrayList<String> defcategories = new ArrayList<>();
        try {
            defPack.load();
            for(CategoryDeclaration decl : defPack.getDefinedCategories()) {
                defcategories.add(decl.getCategory().toLowerCase());
            }
        } catch (IOException e) {
            defcategories.addAll(Arrays.asList("entity, block, item, particle, enchantment, dimension, effect, difficulty, gamemode, gamerule, slot".split(", ")));
            e.printStackTrace();
        }

        this.initialize(defcategories);
    }

    public NBTTMLexerProfile(CommandModule module) {
        ArrayList<String> defcategories = new ArrayList<>();
        module.getAllNamespaces().forEach(n -> n.getTypeManager().getAllDictionaries().forEach(d -> {
            if(!defcategories.contains(d.getCategory())) defcategories.add(d.getCategory());
        }));

        this.initialize(defcategories);
    }

    private void initialize(Collection<String> defcategories) {
        contexts.add(new StringTypeMatchLexerContext(
                new String[] {",", ":", "#", "*"},
                new TokenType[] {COMMA, COLON, HASH, WILDCARD}
        ));
        contexts.add(new StringMatchLexerContext(BRACE, "{", "}", "[", "]", "(", ")"));
        contexts.add(new StringMatchLexerContext(PRIMITIVE_TYPE, "Byte", "Short", "Int", "Float", "Double", "Long", "String", "Boolean"));
        contexts.add(new IdentifierLexerContext(REFERENCE, "[a-zA-Z0-9_]", "\\$"));
        contexts.add(new IdentifierLexerContext(KEY, "[a-zA-Z,0-9_]"));
        contexts.add(new IdentifierLexerContext(IDENTIFIER, "[a-zA-Z,0-9_]"));

        contexts.add(new StringMatchLexerContext(DEFINITION_CATEGORY, defcategories.toArray(new String[0])));


        //String literals
        contexts.add(new LexerContext() {

            String delimiters = "\"";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(str.length() <= 0) return new ScannerContextResponse(false);
                char startingCharacter = str.charAt(0);

                if(delimiters.contains(Character.toString(startingCharacter))) {

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
                            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, STRING_LITERAL, escapedChars);
                            response.setError("Illegal line end in string literal", i, 1);
                            return response;
                        }
                        token.append(c);
                        if(c == '\\') {
                            token.append(str.charAt(i+1));
                            escapedChars.put(new TokenSection(i,2), "string_literal.escape");
                            i++;
                        } else if(c == startingCharacter) {
                            return new ScannerContextResponse(true, token.toString(), end, STRING_LITERAL, escapedChars);
                        }
                    }
                    //Unexpected end of input
                    ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, STRING_LITERAL, escapedChars);
                    response.setError("Unexpected end of input", str.length()-1, 1);
                    return response;
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(STRING_LITERAL);
            }
        });

        //Comments
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(!str.startsWith("#")) return new ScannerContextResponse(false);
                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), COMMENT);
                } else return new ScannerContextResponse(true, str, COMMENT);
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
    }

    @Override
    public boolean canMerge(char ch0, char ch1) {
        return Character.isJavaIdentifierPart(ch0) && Character.isJavaIdentifierPart(ch1);
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","nbttm");
        header.attributes.put("DESC","NBT Type Map File");
    }
}
