package com.daviipkp.stevecommandlib2;

import com.daviipkp.stevecommandlib2.instance.*;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SteveCommandLib2 {

    private static final List<QueuedCommand> queuedCommands = new CopyOnWriteArrayList<>();
    private static final List<TriggeredCommand> triggeredCommands = new CopyOnWriteArrayList<>();
    private static ExecutorService pool;
    private static long threadTPS;
    private static long lastTickTime = 0;
    private static boolean shouldTick = false;
    private static boolean debug = false;

    /**
     * Initializes the lib engine
     *
     * @param nThreads  Number of threads to use in the parallel command pool
     * @param threadTPS Target Ticks Per Second for the execution loops
     */
    public SteveCommandLib2(int nThreads, int threadTPS) {
        if (pool == null || pool.isShutdown()) {
            pool = Executors.newFixedThreadPool(nThreads);
        }
        SteveCommandLib2.threadTPS = threadTPS;
        startMainThread();
    }

    /**
     * Enables or disables debug logging to the system console
     *
     * @param debug True to enable logs, false to silence them
     */
    public static void debug(boolean debug) {
        SteveCommandLib2.debug = debug;
    }

    /**
     * Sets the directory where the PythonManager looks for scripts
     *
     * @param folder The folder containing .py scripts.
     */
    public static void setPythonScriptsFolder(File folder) {
        PythonManager.setScriptFolder(folder);
    }

    /**
     * Adds a command to the execution pipeline based on its type
     *
     * @param command The command instance (Queued, Parallel, or Triggered)
     * @throws IllegalStateException if the command type is unknown
     */
    public static void addCommand(Command command) {
        switch (command) {
            case QueuedCommand q -> {
                queuedCommands.add(q);
                systemPrint("Added queued command " + command.getClass().getSimpleName() + " to list.");
            }
            case ParallelCommand p -> {
                p.start();
                addToParallelPool(p); // Actually submit to the pool
                systemPrint("Added parallel command " + command.getClass().getSimpleName() + " to thread pool.");
            }
            case TriggeredCommand t -> {
                t.start();
                triggeredCommands.add(t);
                systemPrint("Added triggered command " + command.getClass().getSimpleName() + " to list.");
            }
            default -> throw new IllegalStateException("Unexpected command value: " + command);
        }
    }

    /**
     * The main logic tick. Processes the queued list and the triggered list
     *
     * @param tickDelta Time in milliseconds since the last tick
     */
    public static void tick(long tickDelta) {
        if (!queuedCommands.isEmpty()) {
            QueuedCommand q = queuedCommands.getFirst();

            if (!q.isFinished()) {
                if (!q.isRunning()) {
                    q.start();
                }
                q.execute(tickDelta);
                if (q.isFinished()) {
                    queuedCommands.removeFirst();
                }
            } else {
                queuedCommands.removeFirst();
            }
        }
        if (!triggeredCommands.isEmpty()) {
            for (TriggeredCommand command : triggeredCommands) {
                command.tick(tickDelta);
                if (command.isFinished()) {
                    triggeredCommands.remove(command);
                }
            }
        }
    }

    public static void systemPrint(String s) {
        if (debug) {
            System.out.println("[SteveCommandLib2] " + s);
        }
    }

    /**
     * Submits a ParallelCommand to the ExecutorService
     * The command runs in its own thread loop until finished
     *
     * @param command The parallel command to execute
     */
    public static void addToParallelPool(ParallelCommand command) {
        pool.submit(() -> {
            long lastTime = System.currentTimeMillis();

            try {
                while (command.isRunning()) {
                    long now = System.currentTimeMillis();
                    long localDelta = now - lastTime;
                    long targetFrameTime = 1000 / (threadTPS > 0 ? threadTPS : 1);

                    if (localDelta >= targetFrameTime) {
                        command.execute(localDelta);
                        lastTime = now;
                    } else {
                        Thread.sleep(1);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in parallel command execution: " + e.getMessage());
                e.printStackTrace();
                command.finish();
            }
        });
    }

    public static long getThreadTPS() {
        return threadTPS;
    }

    public static void setThreadTPS(long threadTPS) {
        SteveCommandLib2.threadTPS = threadTPS;
    }

    /**
     * Starts the main processing loop in a separate thread
     * This loop calls the static tick() method
     */
    public void startMainThread() {
        shouldTick = true;
        lastTickTime = System.currentTimeMillis();

        pool.submit(() -> {
            while (shouldTick) {
                long now = System.currentTimeMillis();
                long delta = now - lastTickTime;
                tick(delta);

                lastTickTime = now;


                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stopMainThread();
                }
            }
        });
    }

    /**
     * Stops the main processing loop
     */
    public void stopMainThread() {
        shouldTick = false;
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
        }
    }
}