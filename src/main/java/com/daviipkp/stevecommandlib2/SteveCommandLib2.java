package com.daviipkp.stevecommandlib2;

import com.daviipkp.stevecommandlib2.instance.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
public class SteveCommandLib2 {

    private static final Logger LOGGER = Logger.getLogger(SteveCommandLib2.class.getName());

    // Command Storage
    private final List<QueuedCommand> queuedCommands = new CopyOnWriteArrayList<>();
    private final List<TriggeredCommand> triggeredCommands = new CopyOnWriteArrayList<>();

    // Threading & Execution
    private final ExecutorService pool;
    private final AtomicLong threadTPS = new AtomicLong(20);
    private final AtomicBoolean shouldTick = new AtomicBoolean(false);

    // Metrics & State
    private long lastTickTime = 0;
    private final AtomicLong commandsExecuted = new AtomicLong(0);
    private final AtomicLong commandsFailed = new AtomicLong(0);
    private static boolean debugMode = false;

    /**
     * Private constructor to enforce the use of the Builder
     */
    private SteveCommandLib2(int nThreads, long tps, boolean debug, File scriptFolder) {
        this.pool = Executors.newFixedThreadPool(nThreads);
        this.threadTPS.set(tps);
        this.debugMode = debug;

        if (scriptFolder != null) {
            PythonManager.setScriptFolder(scriptFolder);
        }

        if (debugMode) {
            LOGGER.setLevel(Level.ALL);
            LOGGER.info("SteveCommandLib2 initialized in DEBUG mode with " + nThreads + " threads.");
        }
    }

    /**
     * Adds a command to the execution pipeline based on its type
     *
     * @param command The command instance
     * @throws IllegalArgumentException if the command type is unknown or null
     */
    public void addCommand(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Cannot add a null command to the pipeline.");
        }

        switch (command) {
            case QueuedCommand q -> {
                queuedCommands.add(q);
                logDebug("Added QueuedCommand: " + command.getClass().getSimpleName() + " (Queue size: " + queuedCommands.size() + ")");
            }
            case ParallelCommand p -> {
                p.start();
                addToParallelPool(p);
                logDebug("Submitted ParallelCommand: " + command.getClass().getSimpleName() + " to thread pool.");
            }
            case TriggeredCommand t -> {
                t.start();
                triggeredCommands.add(t);
                logDebug("Registered TriggeredCommand: " + command.getClass().getSimpleName());
            }
            default -> {
                commandsFailed.incrementAndGet();
                throw new IllegalArgumentException("Unsupported command type: " + command.getClass().getName());
            }
        }
    }

    /**
     * Cancels and removes all pending queued and triggered commands
     */
    public void flushCommands() {
        queuedCommands.clear();
        triggeredCommands.clear();
        logDebug("All pending commands have been flushed.");
    }

    /**
     * Returns an unmodifiable view of the currently queued commands
     */
    public List<QueuedCommand> getQueuedCommands() {
        return Collections.unmodifiableList(queuedCommands);
    }
    /**
     * Starts the main processing loop in a separate thread
     */
    public void start() {
        if (shouldTick.get()) {
            LOGGER.warning("Attempted to start the engine, but it is already running.");
            return;
        }

        shouldTick.set(true);
        lastTickTime = System.currentTimeMillis();

        pool.submit(() -> {
            Thread.currentThread().setName("SteveLib-MainTick-Thread");
            while (shouldTick.get()) {
                try {
                    long now = System.currentTimeMillis();
                    long delta = now - lastTickTime;

                    tick(delta);
                    lastTickTime = now;
                    long targetFrameTime = 1000L / Math.max(1, threadTPS.get());
                    long processingTime = System.currentTimeMillis() - now;
                    long sleepTime = Math.max(1, targetFrameTime - processingTime);

                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.severe("Main tick thread interrupted!");
                    stop();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Unexpected error in main tick loop", e);
                }
            }
        });
        LOGGER.info("SteveCommandLib2 engine started. Target TPS: " + threadTPS.get());
    }

    /**
     * Safely shuts down the engine and its thread pool.
     */
    public void stop() {
        shouldTick.set(false);
        logDebug("Initiating engine shutdown...");

        pool.shutdown();
        try {
            if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("SteveCommandLib2 engine stopped. Executed: " + commandsExecuted.get() + " | Failed: " + commandsFailed.get());
    }

    private void tick(long tickDelta) {
        processQueuedCommands(tickDelta);
        processTriggeredCommands(tickDelta);
    }

    private void processQueuedCommands(long tickDelta) {
        if (queuedCommands.isEmpty()) return;

        QueuedCommand q = queuedCommands.getFirst();
        try {
            if (!q.isFinished()) {
                if (!q.isRunning()) {
                    q.start();
                }
                q.execute(tickDelta);
                if (q.isFinished()) {
                    queuedCommands.removeFirst();
                    commandsExecuted.incrementAndGet();
                }
            } else {
                queuedCommands.removeFirst();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing QueuedCommand: " + q.getClass().getSimpleName(), e);
            queuedCommands.removeFirst();
            commandsFailed.incrementAndGet();
        }
    }

    private void processTriggeredCommands(long tickDelta) {
        if (triggeredCommands.isEmpty()) return;

        for (TriggeredCommand command : triggeredCommands) {
            try {
                command.tick(tickDelta);
                if (command.isFinished()) {
                    triggeredCommands.remove(command);
                    commandsExecuted.incrementAndGet();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error executing TriggeredCommand: " + command.getClass().getSimpleName(), e);
                triggeredCommands.remove(command);
                commandsFailed.incrementAndGet();
            }
        }
    }

    private void addToParallelPool(ParallelCommand command) {
        pool.submit(() -> {
            Thread.currentThread().setName("SteveLib-Parallel-" + command.getClass().getSimpleName());
            long lastTime = System.currentTimeMillis();

            try {
                while (command.isRunning() && shouldTick.get()) {
                    long now = System.currentTimeMillis();
                    long localDelta = now - lastTime;
                    long targetFrameTime = 1000L / Math.max(1, threadTPS.get());

                    if (localDelta >= targetFrameTime) {
                        command.execute(localDelta);
                        lastTime = now;
                    } else {
                        Thread.sleep(1);
                    }
                }
                commandsExecuted.incrementAndGet();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in parallel command execution: " + command.getClass().getSimpleName(), e);
                command.finish();
                commandsFailed.incrementAndGet();
            }
        });
    }

    private void logDebug(String message) {
        if (debugMode) {
            LOGGER.info("[DEBUG] " + message);
        }
    }

    public static void systemPrint(String message) {
        if (debugMode) {
            LOGGER.info("[SteveCommandLib2] " + message);
        }
    }

    public long getThreadTPS() { return threadTPS.get(); }
    public void setThreadTPS(long tps) { this.threadTPS.set(tps); }
    public void setDebugMode(boolean debug) { debugMode = debug; }
    public long getCommandsExecutedCount() { return commandsExecuted.get(); }

    /**
     * Builder class for creating configured instances
     */
    public static class Builder {
        private int threads = Runtime.getRuntime().availableProcessors();
        private long tps = 20;
        private boolean debug = false;
        private File scriptFolder = null;

        public Builder withThreads(int threads) {
            this.threads = threads;
            return this;
        }

        public Builder withTargetTPS(long tps) {
            this.tps = tps;
            return this;
        }

        public Builder enableDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder withPythonScriptsFolder(File folder) {
            this.scriptFolder = folder;
            return this;
        }

        public SteveCommandLib2 build() {
            return new SteveCommandLib2(threads, tps, debug, scriptFolder);
        }
    }
}