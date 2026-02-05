package com.daviipkp.stevecommandlib2;

import com.daviipkp.stevecommandlib2.instance.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SteveCommandLib2 {

    private static final List<QueuedCommand> queuedCommands = new CopyOnWriteArrayList<>();
    private static final List<TriggeredCommand> triggeredCommands = new CopyOnWriteArrayList<>();

    private static ExecutorService pool;

    private static long threadTPS;

    private static long DELTA = 0;
    private static long lastTickTime = 0;

    private static boolean shouldTick = false;
    private static boolean debug = false;
    public static void debug(boolean debug) {
        SteveCommandLib2.debug = debug;
    }

    public SteveCommandLib2(int nThreads, int threadTPS) {
        pool = Executors.newFixedThreadPool(nThreads);
        SteveCommandLib2.threadTPS = threadTPS;
        startMainThread();
    }

    public static void addCommand(Command arg0) {
        switch(arg0) {
            case QueuedCommand q -> {
                queuedCommands.add(((QueuedCommand) arg0));
                systemPrint("Added queued command " + arg0.getClass().getSimpleName() + " to list!");
            }
            case ParallelCommand q -> {
                arg0.start();
                systemPrint("Added parallel command " + arg0.getClass().getSimpleName() + " to thread pool!");
            }
            case TriggeredCommand q -> {
                arg0.start();
                triggeredCommands.add(((TriggeredCommand)arg0));
                systemPrint("Added triggered command " + arg0.getClass().getSimpleName() + " to list!");
            }
            default -> throw new IllegalStateException("Unexpected value: " + arg0);
        }
    }
    public static void tick(long tickDelta) {
        if(!queuedCommands.isEmpty()) {
            QueuedCommand q = queuedCommands.getFirst();
            if(!q.isFinished()){
                if(!q.isRunning()){
                    q.start();
                }
                q.execute(tickDelta);
                if(q.isFinished()) {
                   queuedCommands.removeFirst();
                }
            }else{
                queuedCommands.removeFirst();
            }
        }

        if(!triggeredCommands.isEmpty()) { 
            triggeredCommands.forEach((command) -> {
               command.tick(tickDelta);
               if(command.isFinished())triggeredCommands.remove(command);
            });
        }
    }


    public static void systemPrint(String s) {
        if(debug) {
            System.out.println("[SteveCommandLib2] " + s);
        }
    }

    public static void addToParallelPool(ParallelCommand arg0) {
        pool.submit(() -> {
            try {
                while(arg0.isRunning()) {
                    if(DELTA >= 1000/threadTPS) {
                        arg0.execute(DELTA);
                        DELTA = 0;
                    }
                }
            } catch (Exception e) {
                arg0.finish();
            }
        });
    }

    public static long getThreadTPS() {
        return threadTPS;
    }

    public static void setThreadTPS(long threadTPS) {
        SteveCommandLib2.threadTPS = threadTPS;
    }

    public void startMainThread() {
        shouldTick=true;
        pool.submit(new Runnable() {
            @Override
            public void run() {
                while(shouldTick) {
                    tick((lastTickTime == 0)?(0):(System.currentTimeMillis() - lastTickTime));
                    lastTickTime = System.currentTimeMillis();
                }
            }
        });
    }

    public void stopMainThread() {
        shouldTick = false;
    }

}
