package com.rsr007.loghandler.impl;

import com.rsr007.loghandler.core.LogTask;

public class DbLogger {

    public void log(LogTask task) {
        System.out.println("DB "+ task.timestamp() + " "
                + task.level()+ " ["
                + task.loggerName()+ "] "
                + task.message()
        );
    }
}
