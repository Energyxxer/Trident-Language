package com.energyxxer.trident.compiler.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonTraverser {
    public static final JsonTraverser INSTANCE = new JsonTraverser(null);

    private JsonElement head;

    public JsonTraverser(JsonElement head) {
        this.head = head;
    }

    public JsonTraverser reset(JsonElement newHead) {
        this.head = newHead;
        return this;
    }

    public JsonTraverser get(String key) {
        if(head != null && head.isJsonObject() && head.getAsJsonObject().has(key)) {
            head = head.getAsJsonObject().get(key);
        } else deadEnd();
        return this;
    }

    public JsonTraverser get(int index) {
        if(head != null && index >= 0 && head.isJsonArray() && head.getAsJsonArray().size() > index) {
            head = head.getAsJsonArray().get(index);
        } else deadEnd();
        return this;
    }

    public JsonObject asJsonObject() {
        if(head != null && head.isJsonObject()) return (JsonObject) head;
        return null;
    }

    public JsonArray asJsonArray() {
        if(head != null && head.isJsonArray()) return (JsonArray) head;
        return null;
    }

    public String asString() {
        return asString(null);
    }

    public String asString(String defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isString()) return head.getAsString();
        return defaultValue;
    }

    public boolean asBoolean() {
        return asBoolean(false);
    }

    public boolean asBoolean(boolean defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isBoolean()) return head.getAsBoolean();
        return defaultValue;
    }

    public byte asByte() {
        return asByte((byte) 0);
    }

    public byte asByte(byte defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsByte();
        return defaultValue;
    }

    public short asShort() {
        return asShort((short) 0);
    }

    public short asShort(short defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsShort();
        return defaultValue;
    }

    public int asInt() {
        return asInt(0);
    }

    public int asInt(int defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsInt();
        return defaultValue;
    }

    public long asLong() {
        return asLong(0);
    }

    public long asLong(long defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsLong();
        return defaultValue;
    }

    public float asFloat() {
        return asFloat(0);
    }

    public float asFloat(float defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsFloat();
        return defaultValue;
    }

    public double asDouble() {
        return asDouble(0);
    }

    public double asDouble(double defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsDouble();
        return defaultValue;
    }

    public BigInteger asBigInt() {
        return asBigInt(BigInteger.ZERO);
    }

    public BigInteger asBigInt(BigInteger defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsBigInteger();
        return defaultValue;
    }

    public BigDecimal asBigDecimal() {
        return asBigDecimal(BigDecimal.ZERO);
    }

    public BigDecimal asBigDecimal(BigDecimal defaultValue) {
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsBigDecimal();
        return defaultValue;
    }

    private void deadEnd() {
        head = null;
    }
}
