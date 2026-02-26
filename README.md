# SteveCommandLib2
SteveCommandLib2 is a multithread command engine for Java. It helps you manage complex command pipelines so you can run tasks one after another, in parallel threads, or using some specific triggers.
It also has built in stuff to run Python scripts right inside your Java app, and a dynamic JSON registry to parse and create commands on the fly.

## Core Features
- Tick-based engine with configurable TPS.

- Three main command types: Queued, Parallel, and Triggered.

- Thread-safe command pooling and running.

- Python integration (using Jep) to load, check, and run .py scripts with context variables.

- JSON-to-Command mapping (using Jackson) to auto create Java command classes from JSON strings.

## Setting up the Engine
The main engine is instantiated using a Builder pattern. This allows you to configure the thread pool, target TPS, and debug mode before starting the main loop.

```
import com.daviipkp.stevecommandlib2.SteveCommandLib2;

// Initialize the engine
SteveCommandLib2 engine = new SteveCommandLib2.Builder()
    .withThreads(4)           // Number of threads in the pool
    .withTargetTPS(20)        // Target execution speed
    .enableDebug(true)        // Prints internal logs
    .build();

// Start the execution loop
engine.start();
```

When you are done, or if the application is shutting down, gracefully stop the engine:
```
engine.stop();
```
### Command Types
You can add commands to the engine using engine.addCommand(yourCommand). The engine handles them differently based on their base class:

- QueuedCommand: These commands are added to a single-file line. The engine will execute the first one in the queue, wait for it to finish (when isFinished() returns true), and only then move on to the next.

- ParallelCommand: These commands are immediately submitted to the thread pool. They run concurrently in their own loop until isRunning() is set to false (usually by calling stop()).

- TriggeredCommand: These are kept in a separate list and get ticked every cycle. They are useful for commands that need to wait for a specific condition or event before doing their job.

### Python Integration
If your project requires running external Python scripts, use the PythonManager. It scans a folder for .py files, reads their required context variables (if they define a REQUIRED_VARS list), and executes them safely.

```
import com.daviipkp.stevecommandlib2.PythonManager;
import java.io.File;
import java.util.Map;

PythonManager.setScriptFolder(new File("/path/to/scripts"));
PythonManager.loadScripts();

// Execute a script passing the required variables in a Map
Map<String, Object> context = Map.of("player_name", "Steve", "health", 100);
PythonManager.executeScript("heal_player.py", context);
```
### JSON Registry
The Jsoning class makes it easy to convert text into runnable commands. If you annotate your command classes with @CommandDescribe, you can register an entire package at once.
```
import com.daviipkp.stevecommandlib2.Jsoning;
import com.daviipkp.stevecommandlib2.instance.Command;

// Scan and register all annotated commands in this package
Jsoning.registerCommandPackage("com.yourproject.commands");

// Later, you can create a command instance directly from a JSON string
String rawJson = "{\"type\": \"heal\", \"amount\": 20}";
Command myCommand = Jsoning.createCommandFromJson(rawJson);

engine.addCommand(myCommand);
```
