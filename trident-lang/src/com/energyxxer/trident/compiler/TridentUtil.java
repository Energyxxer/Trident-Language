package com.energyxxer.trident.compiler;

import com.energyxxer.enxlex.lexical_analysis.token.Token;

public class TridentUtil {
    /**
     * TridentUtil should not be instantiated.
     * */
    private TridentUtil() {
    }

    public static class ResourceLocation {
        public String namespace;
        public String body;

        public ResourceLocation(Token rlToken) {
            if(rlToken.value.contains(":")) {
                namespace = rlToken.value.substring(0, rlToken.value.indexOf(":"));
                body = rlToken.value.substring(rlToken.value.indexOf(":"));
            } else {
                namespace = "minecraft";
                body = rlToken.value;
            }
        }
    }
}
