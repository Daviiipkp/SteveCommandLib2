package com.daviipkp.stevecommandlib2;

import com.daviipkp.stevecommandlib2.annotations.CommandDescribe;
import com.daviipkp.stevecommandlib2.annotations.FieldDescribe;
import com.daviipkp.stevecommandlib2.instance.Command;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jsoning {

    private static final List<Class<? extends Annotation>> annotations = new ArrayList<>();
    private static final ObjectMapper MAPPER = createDefaultMapper();
    private static final List<Class<? extends Command>> implementations = new ArrayList<>();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static void registerCommandPackage(String packageName) {
        implementations.addAll(getRegisteredCommands(packageName));
    }

    public static List<Class<? extends Command>> getLoadedCommands() {
        return implementations;
    }

    private static ObjectMapper createDefaultMapper() {
        Reflections reflections = new Reflections("com.daviipkp.stevecommandlib2.annotations");
        annotations.addAll(reflections.getSubTypesOf(Annotation.class));

        ObjectMapper mapper = new ObjectMapper();

        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

    private static List<Class<? extends Command>> getRegisteredCommands(String... packages) {
        List<Class<? extends Command>> list = new ArrayList<>();
        for(String s : packages) {
            for(Class<?> c : new Reflections(s).getTypesAnnotatedWith(CommandDescribe.class)) {
                if(Command.class.isAssignableFrom(c)) {
                    list.add(c.asSubclass(Command.class));
                }else {
                    System.out.println(">>> Ignoring command '" + c.getName() + "' because it does not extend Command class.");
                }
            }
        }
        return list;
    }

    public static String stringify(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error on stringify: ", e);
        }
    }

    public static <T> T parse(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error parsing: " + json, e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> tr) {
        try {
            return MAPPER.readValue(json, tr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error parsing: " + json, e);
        }
    }

    public static String valueAtPath(String path, String json) throws JsonProcessingException {
        JsonNode root = MAPPER.readTree(json);
        String innerJson = root.at(path).asText();
        return innerJson;
    }

    public static String generateGuide(Class<?> arg0) {
        Class<? extends Annotation > ann = checkForAnnotation(arg0);
        StringBuilder b = new StringBuilder();
        var snakeCase = new PropertyNamingStrategies.SnakeCaseStrategy();
        b.append("{\n");
        b.append(formattedLine("type", arg0.getSimpleName(), 1, false, true));
        b.append(formattedLine("description", arg0.getAnnotation(CommandDescribe.class).description(), 1, false, true));
        List<String> pArgs = new ArrayList<>();

        int counter = 0;
        Field[] fields = arg0.getDeclaredFields();
        Map<Field, String> list = new HashMap<>();
        for(Field f : fields) {
            if(f.isAnnotationPresent(FieldDescribe.class)) {
                list.put(f, f.getAnnotation(FieldDescribe.class).description());
            }
        }
        if(!list.isEmpty()) {
            b.append(openKey("arguments", 1));
            for(Field s : list.keySet()) {
                if(list.get(s).length() > 0) {
                    b.append(formattedLine(s.getName(), list.get(s), 2, (counter>(list.size())), true));
                }else{
                    b.append(formattedLine(s.getName(), "<" + s.getType().getName() + ">", 2, (counter>(list.size())), true));
                }
                counter++;

            }
            b.append(closeKey(1));
        }

        b.append(closeKey(0));

        return b.toString();
    }

    public static String tab(int arg0) {
        StringBuilder b = new StringBuilder();
        b.append("    ".repeat(Math.max(0, arg0)));
        return b.toString();
    }

    public static String lineBreak() {
        return "\n";
    }

    public static String doubleQuoted(String arg0) {
        return "\"" + arg0 + "\"";
    }

    public static String formattedLine(String key, String value, int tabs, boolean last, boolean doBreak) {
        return tab(tabs) + doubleQuoted(key) + ": " + doubleQuoted(value) + (last?"":",") + (doBreak?"\n":"");
    }

    public static String openKey(String key, int tabs) {
        return tab(tabs) + doubleQuoted(key) + ": {" + lineBreak();
    }
    public static String closeKey(int tabs) {
        return tab(tabs) + "}" + lineBreak();
    }


    public static Class<? extends Annotation> checkForAnnotation(Class<?> clazz) {
        if(clazz == null ) {
            throw new IllegalArgumentException("Trying to parse a null value");
        }
        for(Class<? extends Annotation> annotation : annotations) {
            if(clazz.isAnnotationPresent(annotation)) {
                return annotation;
            }
        }
        throw new IllegalArgumentException("No annotation found for " + clazz);

    }

}
