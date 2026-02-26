package com.daviipkp.stevecommandlib2;

import jep.JepConfig;
import jep.JepException;
import jep.SubInterpreter;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PythonManager {

    public static class ScriptInfo {
        private final File file;
        private final List<String> requiredVars;

        public ScriptInfo(File file, List<String> requiredVars) {
            this.file = file;
            this.requiredVars = requiredVars != null ? requiredVars : new ArrayList<>();
        }

        public File getFile() { return file; }
        public List<String> getRequiredVars() { return requiredVars; }
    }

    private static File scriptFolder;
    private static final Map<String, ScriptInfo> loadedScripts = new ConcurrentHashMap<>();

    /**
     * @param folder Folder to search for Python Scripts
     */
    public static void setScriptFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            scriptFolder = folder;
            return;
        }
        throw new IllegalArgumentException("Invalid script folder");
    }

    /**
     * Loads all .py files as executable scripts in ScriptFolder
     */
    public static void loadScripts() {
        if (scriptFolder == null) {
            return;
        }
        loadedScripts.clear();
        File[] files = scriptFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".py"));

        if (files == null) return;

        JepConfig config = new JepConfig();
        config.addIncludePaths(scriptFolder.getAbsolutePath());

        for (File file : files) {
            try (SubInterpreter jep = new SubInterpreter(config)) {
                jep.runScript(file.getAbsolutePath());

                Object reqObj = jep.getValue("REQUIRED_VARS");
                List<String> requirements = new ArrayList<>();

                if (reqObj instanceof List) {
                    for (Object o : (List<?>) reqObj) {
                        requirements.add(o.toString());
                    }
                }
                loadedScripts.put(file.getName(), new ScriptInfo(file, requirements));
                SteveCommandLib2.systemPrint("Loaded " + file.getName() + " needing: " + requirements);

            } catch (Exception e) {
                System.err.println("Failed to load metadata for " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Searches for required vars (any variable inside an array called REQUIRED_VARS) in referred Script
     *
     * @param scriptName Script to get requirements
     */
    public static List<String> getScriptRequirements(String scriptName) {
        if (loadedScripts.containsKey(scriptName)) {
            return new ArrayList<>(loadedScripts.get(scriptName).getRequiredVars());
        }
        return Collections.emptyList();
    }


    /**
     * @param scriptName Name of the Script to be executed
     * @param context Defining the variables required (REQUIRED_VARS) to execute
     */
    public static void executeScript(String scriptName, Map<String, Object> context) {
        ScriptInfo info = loadedScripts.get(scriptName);

        if (info == null) {
            System.err.println("Script not found or not loaded: " + scriptName);
            return;
        }

        if (!validateContext(info, context)) {
            System.err.println("Aborting execution of " + scriptName + ". Missing required variables in context.");
            return;
        }

        JepConfig config = new JepConfig();
        config.addIncludePaths(scriptFolder.getAbsolutePath());

        try (SubInterpreter jep = new SubInterpreter(config)) {
            if (context != null) {
                for (Map.Entry<String, Object> entry : context.entrySet()) {
                    jep.set(entry.getKey(), entry.getValue());
                }
            }
            jep.runScript(info.getFile().getAbsolutePath());
            jep.invoke("main");

        } catch (JepException e) {
            System.err.println("Error executing script " + scriptName);
            e.printStackTrace();
        }
    }

    // TODO: use thenAccept/exceptionally
    public static CompletableFuture<Void> executeScriptAsync(String scriptName, Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> executeScript(scriptName, context));
    }

    private static boolean validateContext(ScriptInfo info, Map<String, Object> context) {
        if (info.getRequiredVars().isEmpty()) return true;

        if (context == null) {
            SteveCommandLib2.systemPrint("Warning: Context is null but script requires: " + info.getRequiredVars());
            return false;
        }

        for (String req : info.getRequiredVars()) {
            if (!context.containsKey(req)) {
                SteveCommandLib2.systemPrint("Missing variable in python context: " + req);
                return false;
            }
        }
        return true;
    }

    public static List<String> getLoadedScriptNames() {
        return new ArrayList<>(loadedScripts.keySet());
    }
}