package com.daviipkp.stevecommandlib2.instance;

public abstract class TriggeredCommand extends Command {

    private long timeBetweenChecks = 1000;
    private long timer = 0;

    public void tick(long delta) {
        timer += delta;
        if(timer>=timeBetweenChecks) {
            timer = 0;
            if(checkTrigger()) {
                execute(delta);
            }
        }
    }

    public abstract boolean checkTrigger();

    @Override
    public void execute(long delta) {
        super.execute(delta);
        finish();
    }
    protected void setTimeBetweenChecks(long arg0) {
        timeBetweenChecks = arg0;
    }
}
