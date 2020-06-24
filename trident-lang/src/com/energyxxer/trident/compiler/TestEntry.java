package com.energyxxer.trident.compiler;

import com.energyxxer.trident.compiler.util.JsonTraverser;
import com.google.gson.JsonObject;

public class TestEntry {
    public static void main(String[] args) {
        JsonObject root = new JsonObject();
        JsonTraverser.INSTANCE.reset(root).createOnTraversal().get("output").get("export-comments").asBoolean(false);

        System.out.println(root);
    }
}
