package com.energyxxer.trident.compiler;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import java.util.Objects;

public class TridentUtil {
    /**
     * TridentUtil should not be instantiated.
     * */
    private TridentUtil() {
    }

    public static class ResourceLocation {
        public String namespace;
        public String body;

        public ResourceLocation(TokenPattern<?> typeGroup) {
            TokenPattern<?> namespacePattern = typeGroup.find("NAMESPACE");
            namespace = namespacePattern != null ? namespacePattern.flatten(false) : "minecraft";
            body = typeGroup.find("TYPE_NAME").flatten(true);
        }

        public ResourceLocation(Token token) {
            this(token.value);
        }

        public ResourceLocation(String str) {
            if(str.contains(":")) {
                namespace = str.substring(0, str.indexOf(":"));
                body = str.substring(str.indexOf(":")+1);
            } else {
                namespace = "minecraft";
                body = str;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResourceLocation that = (ResourceLocation) o;
            return Objects.equals(namespace, that.namespace) &&
                    Objects.equals(body, that.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, body);
        }

        @Override
        public String toString() {
            return namespace + ":" + body;
        }
    }
}
