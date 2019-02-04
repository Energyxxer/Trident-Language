package com.energyxxer.trident.compiler;

import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

import java.util.Objects;

public class TridentUtil {
    /**
     * TridentUtil should not be instantiated.
     * */
    private TridentUtil() {
    }

    public static class ResourceLocation {
        public boolean isTag;
        public String namespace;
        public String body;

        public ResourceLocation(TokenPattern<?> typeGroup) {
            if(typeGroup.find("") != null) isTag = true;
            TokenPattern<?> namespacePattern = typeGroup.find("NAMESPACE");
            namespace = namespacePattern != null ? namespacePattern.flattenTokens().get(0).value : "minecraft";
            body = typeGroup.find("TYPE_NAME").flatten(true);
        }

        public ResourceLocation(Token token) {
            this(token.value);
        }

        public ResourceLocation(String str) {
            if(str.startsWith("#")) {
                isTag = true;
                str = str.substring(1);
            }
            if(str.contains(":")) {
                namespace = str.substring(0, str.indexOf(":"));
                body = str.substring(str.indexOf(":")+1);
            } else {
                namespace = "minecraft";
                body = str;
            }
        }

        public static ResourceLocation createStrict(String str) {
            ScannerContextResponse valueResult = TridentLexerProfile.usefulContexts.get(TridentTokens.RESOURCE_LOCATION).analyzeExpectingType(str, null, null);
            if(valueResult.success && valueResult.endLocation.index == str.length()) {
                return new TridentUtil.ResourceLocation(str);
            } else return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResourceLocation location = (ResourceLocation) o;
            return isTag == location.isTag &&
                    Objects.equals(namespace, location.namespace) &&
                    Objects.equals(body, location.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isTag, namespace, body);
        }

        @Override
        public String toString() {
            return namespace + ":" + body;
        }

        public void assertStandalone(TokenPattern<?> pattern, ISymbolContext ctx) {
            if(this.isTag) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Expected standalone resource location, instead got tag", pattern, ctx);
            }
        }
    }
}
