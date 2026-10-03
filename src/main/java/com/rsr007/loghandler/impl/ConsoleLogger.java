package com.rsr007.loghandler.impl;

import com.rsr007.loghandler.core.LogTask;

public class ConsoleLogger {

    public void log(LogTask task) {
        System.out.println("CONSOLE: "+ task.timestamp() + " "
                        + task.level()+ " ["
                        + task.loggerName()+ "] "
                        + task.message()
        );
    }
}
