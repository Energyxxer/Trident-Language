package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatures;
import com.energyxxer.trident.compiler.plugin.TridentPlugin;

import java.util.Map;

public class TridentCompilerResources {
    public DefinitionPack[] defaultDefinitionPacks;
    public DefinitionPack[] definitionPacks;
    public Map<String, DefinitionPack> definitionPackAliases;
    public VersionFeatures featureMap;
    public Map<String, TridentPlugin> pluginAliases;
    public String[] rawTypeMaps;

    public TridentCompilerResources shallowClone() {
        TridentCompilerResources copy = new TridentCompilerResources();
        copy.defaultDefinitionPacks = this.defaultDefinitionPacks;
        copy.definitionPacks = this.definitionPacks;
        copy.definitionPackAliases = this.definitionPackAliases;
        copy.featureMap = this.featureMap;
        copy.pluginAliases = this.pluginAliases;
        copy.rawTypeMaps = this.rawTypeMaps;
        return copy;
    }
}
