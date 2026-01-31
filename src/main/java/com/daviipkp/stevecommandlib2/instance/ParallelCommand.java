package com.daviipkp.stevecommandlib2.instance;

import com.daviipkp.stevecommandlib2.SteveCommandLib2;

public abstract class ParallelCommand extends Command {

    @Override
    public void start() {
        super.start();
        SteveCommandLib2.addToParallelPool(this);
    }

    @Override
    public void stop() {
        super.stop();
        Thread.currentThread().interrupt();
    }
}
