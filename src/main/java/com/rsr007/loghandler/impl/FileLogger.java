package com.rsr007.loghandler.impl;

import com.rsr007.loghandler.core.LogTask;

public class FileLogger {

    public void log(LogTask task) {
        System.out.println("FILE: "+task.timestamp() + " "
                + task.level()+ " ["
                + task.loggerName()+ "] "
                + task.message()
        );
    }
}
