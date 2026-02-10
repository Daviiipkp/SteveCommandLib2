package com.daviipkp.stevecommandlib2;

import com.daviipkp.stevecommandlib2.annotations.CommandDescribe;
import com.daviipkp.stevecommandlib2.annotations.FieldDescribe;
import com.daviipkp.stevecommandlib2.instance.Command;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Jsoning {

    private static final ObjectMapper MAPPER;
    private static final List<Class<? extends Command>> loadedCommands = new ArrayList<>();
    static {
        MAPPER = new ObjectMapper();


        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print by default

        MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Scans the specified package for classes annotated with @CommandDescribe
     * that extend the Command class, and registers them.
     *
     * @param packageName The package to scan (e.g., "com.myapp.commands")
     */
    public static void registerCommandPackage(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(CommandDescribe.class);

        for (Class<?> clazz : annotatedClasses) {
            if (Command.class.isAssignableFrom(clazz)) {
                loadedCommands.add(clazz.asSubclass(Command.class));
            } else {
                System.out.println(">>> Ignoring command '" + clazz.getName() + "' because it does not extend Command class.");
            }
        }
    }

    /**
     * @return An unmodifiable list of currently registered command classes
     */
    public static List<Class<? extends Command>> getLoadedCommands() {
        return Collections.unmodifiableList(loadedCommands);
    }

    /**
     * Converts a Java object into a JSON String
     *
     * @param object The object to serialize
     * @return The JSON string representation
     * @throws RuntimeException if serialization fails
     */
    public static String stringify(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error executing stringify.", e);
        }
    }

    /**
     * Parses a JSON string into a specific Java class
     *
     * @param json  The JSON string
     * @param clazz The target class
     * @return The deserialized object
     */
    public static <T> T parse(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Parses a JSON string using a generic TypeReference (useful for Lists!)
     *
     * @param json The JSON string
     * @param tr   The TypeReference definition
     * @return The deserialized object
     */
    public static <T> T parse(String json, TypeReference<T> tr) {
        try {
            return MAPPER.readValue(json, tr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON with TypeReference.", e);
        }
    }

    /**
     * Extracts a specific value from a JSON string using a pointer path
     *
     * @param path The path to the value (e.g., "/user/name")
     * @param json The raw JSON string
     * @return The text value at the specified path
     * @throws JsonProcessingException if the JSON is malformed
     */
    public static String valueAtPath(String path, String json) throws JsonProcessingException {
        JsonNode root = MAPPER.readTree(json);
        JsonNode node = root.at(path);
        return node.isMissingNode() ? null : node.asText();
    }

    /**
     * Generates a JSON guide describing the command structure based on annotations
     *
     * @param clazz The command class to describe
     * @return A pretty-printed JSON string describing the command
     */
    public static String generateGuide(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(CommandDescribe.class)) {
            throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " is not annotated with @CommandDescribe");
        }

        ObjectNode root = MAPPER.createObjectNode();
        CommandDescribe commandInfo = clazz.getAnnotation(CommandDescribe.class);

        root.put("type", clazz.getSimpleName());
        root.put("description", commandInfo.description());

        ObjectNode argumentsNode = MAPPER.createObjectNode();
        boolean hasArguments = false;

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(FieldDescribe.class)) {
                FieldDescribe fieldInfo = field.getAnnotation(FieldDescribe.class);
                String desc = fieldInfo.description();

                if (desc.isEmpty()) {
                    desc = "<" + field.getType().getSimpleName() + ">";
                }

                argumentsNode.put(field.getName(), desc);
                hasArguments = true;
            }
        }

        if (hasArguments) {
            root.set("arguments", argumentsNode);
        }
        return stringify(root);
    }
}