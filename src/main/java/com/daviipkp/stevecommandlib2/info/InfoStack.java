package com.daviipkp.stevecommandlib2.info;

import com.daviipkp.stevecommandlib2.Jsoning;
import com.daviipkp.stevecommandlib2.instance.InfoCommand;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;

public class InfoStack {

    private final Map<String, String> data;

    /**
     * Creates a new InfoStack with a restricted set of allowed keys
     * Initial values for these keys are null
     *
     * @param allowedKeys A list of keys that this stack will support
     */
    public InfoStack(String... allowedKeys) {
        this.data = new LinkedHashMap<>();
        for (String key : allowedKeys) {
            this.data.put(key, null);
        }
    }

    /**
     * Serializes the current stack data into a JSON string
     *
     * @return A formatted JSON string representing the keys and values
     */
    public String getJsonObject() {
        return Jsoning.stringify(data);
    }

    /**
     * Populates this InfoStack from a JSON string
     * Only keys present in the JSON that match the allowed keys of this stack
     * will be updated
     *
     * @param json The JSON string to parse
     * @return The current instance (for chaining)
     */
    public InfoStack fromJson(String json) {
        try {
            Map<String, String> parsed = Jsoning.parse(json, new TypeReference<Map<String, String>>() {});
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                if (this.data.containsKey(entry.getKey())) {
                    this.data.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse JSON into InfoStack: " + e.getMessage());
        }
        return this;
    }

    /**
     * Updates the value of a specific key using the return value of an InfoCommand
     *
     * @param key   The key to update
     * @param value The InfoCommand whose return value will be used
     * @throws IllegalArgumentException If the key was not defined in the constructor
     */
    public void fulfillKey(String key, InfoCommand value) {
        fulfillKey(key, value.getReturn());
    }

    /**
     * Updates the value of a specific key
     *
     * @param key   The key to update
     * @param value The new value
     * @throws IllegalArgumentException If the key was not defined in the constructor
     */
    public void fulfillKey(String key, String value) {
        if (!data.containsKey(key)) {
            throw new IllegalArgumentException("Key '" + key + "' was not found. Check if you added it in the InfoStack constructor.");
        }
        data.put(key, value);
    }

    /**
     * Iterates through all keys and fills any null or empty values
     * with the provided default value
     *
     * @param defaultValue The value to use for missing entries
     */
    public void fulfillEmpty(String defaultValue) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                entry.setValue(defaultValue);
            }
        }
    }
}