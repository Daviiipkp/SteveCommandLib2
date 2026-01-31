package com.daviipkp.stevecommandlib2.instance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandStack extends ParallelCommand {

    private final List<Command> queuedCommands = new CopyOnWriteArrayList<>();

    public CommandStack(Command... commands) {
        this.queuedCommands.addAll(List.of(commands));
    }

    @Override
    public void handleError(Exception e) {

    }

    @Override
    public void execute(long delta) {
        if(!queuedCommands.isEmpty()) {
            if(queuedCommands.get(0).isRunning()) {
                queuedCommands.get(0).execute(delta);
                if(queuedCommands.get(0).isFinished()) {
                    queuedCommands.remove(0);
                }
            }else{
                queuedCommands.get(0).start();
                queuedCommands.get(0).execute(delta);
                if(queuedCommands.get(0).isFinished()) {
                    queuedCommands.remove(0);
                }
            }
        }else{
            finish();
        }
        super.execute(delta);
    }
}
