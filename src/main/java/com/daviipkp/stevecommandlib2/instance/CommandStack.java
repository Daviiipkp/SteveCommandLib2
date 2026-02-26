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
            if(queuedCommands.getFirst().isRunning()) {
                queuedCommands.getFirst().execute(delta);
                if(queuedCommands.getFirst().isFinished()) {
                    queuedCommands.removeFirst();
                }
            }else{
                queuedCommands.getFirst().start();
                queuedCommands.getFirst().execute(delta);
                if(queuedCommands.getFirst().isFinished()) {
                    queuedCommands.removeFirst();
                }
            }
        }else{
            finish();
        }
        super.execute(delta);
    }
}
