package com.daviipkp.stevecommandlib2.instance;


import com.daviipkp.stevecommandlib2.SteveCommandLib2;

public abstract class Command {

    protected boolean finished = false;
    protected boolean running = false;

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        this.finished = true;
        this.running = false;
        SteveCommandLib2.systemPrint("Finished " + this.getClass().getSimpleName());
    }

    public abstract void handleError(Exception e);

    public String getID() {
        return this.getClass().getSimpleName();
    };

    public boolean isRunning() {return running;}

    public void execute(long delta) {
    };

    public void start() {
        if(!finished) {
            running = true;
        }else{
            SteveCommandLib2.systemPrint("Tried to start " + getID() + " but it's already finished.");
            return;
        }
        SteveCommandLib2.systemPrint("Started " + getID());
    }

    public void stop() {
        running = false;
        SteveCommandLib2.systemPrint("Stopped " + getID());
    }


}
