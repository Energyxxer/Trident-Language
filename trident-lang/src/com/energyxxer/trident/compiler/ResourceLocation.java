package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.util.Lazy;

import java.util.Objects;

public class ResourceLocation {
    public boolean isTag;
    public String namespace;
    public String body;

    private Lazy<String[]> parts = new Lazy<>(() -> this.body.split("/", -1));
    private Lazy<ResourceLocation> parent = new Lazy<>(() -> {
        ResourceLocation loc = new ResourceLocation("a:b");
        loc.namespace = this.namespace;
        loc.isTag = this.isTag;
        if(this.body.contains("/")) {
            loc.body = this.body.substring(0, this.body.lastIndexOf("/"));
            return loc;
        } else {
            return null;
        }
    });

    public ResourceLocation(Type type) {
        this.namespace = type.useNamespace() ? type.getNamespace().getName() : "minecraft";
        this.body = type.getName();
    }

    public ResourceLocation(TokenPattern<?> typeGroup) {
        if(typeGroup.find("") != null) isTag = true;
        TokenPattern<?> namespacePattern = typeGroup.find("NAMESPACE");
        namespace = namespacePattern != null ? namespacePattern.flattenTokens().get(0).value : "minecraft";
        body = typeGroup.find("TYPE_NAME").flatten(true);
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
        boolean isTag = str.startsWith("#");
        ScannerContextResponse valueResult = TridentLexerProfile.RESOURCE_LOCATION_CONTEXT.analyzeExpectingType(str.substring(isTag ? 1 : 0), 0, null, null);
        if(valueResult.success && valueResult.endLocation.index == str.length() - (isTag ? 1 : 0)) {
            return new ResourceLocation(str);
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

    public String[] getParts() {
        return parts.getValue();
    }

    public ResourceLocation getParent() {
        return parent.getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(isTag, namespace, body);
    }

    @Override
    public String toString() {
        return (isTag ? "#" : "") + (namespace != null ? namespace + ":" : "") + body;
    }

    public void assertStandalone(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(this.isTag) {
            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Expected standalone resource location, instead got tag", pattern, ctx);
        }
    }
}
