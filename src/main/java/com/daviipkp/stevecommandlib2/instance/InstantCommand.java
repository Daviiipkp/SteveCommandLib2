package com.daviipkp.stevecommandlib2.instance;

public class InstantCommand extends QueuedCommand {

    private Runnable command;

    @Override
    public void handleError(Exception e) {

    }

    public void setCommand(Runnable arg0) {
        command = arg0;
    }

    @Override
    public void start() {
        super.start();
    }


    @Override
    public void execute(long delta) {
        super.execute(delta);
        try {
            command.run();
        }catch (Exception e){
            handleError(e);
        }
        finish();
    }
}
