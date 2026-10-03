package com.rsr007.loghandler.impl;

import com.rsr007.loghandler.core.*;

import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CompositeLogger implements Logger {

    private static final LogWorker LOG_WORKER =
            new LogWorker();

    private final String loggerName;

    public CompositeLogger(Class<?> clazz) {

        this.loggerName = clazz.getName();
    }

    @Override
    public void debug(String message) {

        log(LogLevel.DEBUG, message);
    }

    @Override
    public void info(String message) {

        log(LogLevel.INFO, message);
    }

    @Override
    public void warn(String message) {

        log(LogLevel.WARN, message);
    }

    @Override
    public void error(String message) {

        log(LogLevel.ERROR, message);
    }

    private void log(
            LogLevel level,
            String message
    ) {

        LogTask task = new LogTask(
                level,
                message,
                loggerName,
                Instant.now()
        );

        LOG_WORKER.submit(task);
    }

    public static void shutdown() {
        System.out.println("shutdown called...");
        LOG_WORKER.shutdown();
    }

    public static class LogWorker {

        private static final int QUEUE_SIZE = 1000;

        private final BlockingQueue<LogTask> consoleQueue =
                new ArrayBlockingQueue<>(QUEUE_SIZE);

        private final BlockingQueue<LogTask> fileQueue =
                new ArrayBlockingQueue<>(QUEUE_SIZE);

        private final BlockingQueue<LogTask> dbQueue =
                new ArrayBlockingQueue<>(QUEUE_SIZE);

        private final ConsoleLogger consoleLogger =
                new ConsoleLogger();

        private final FileLogger fileLogger =
                new FileLogger();

        private final DbLogger dbLogger =
                new DbLogger();

        private final Thread consoleThread =
                new Thread(
                        this::processConsole,
                        "console-log-worker"
                );

        private final Thread fileThread =
                new Thread(
                        this::processFile,
                        "file-log-worker"
                );

        private final Thread dbThread =
                new Thread(
                        this::processDb,
                        "db-log-worker"
                );
        //if false, no new log will be accepted
        private volatile boolean acceptingLogs = true;

        public LogWorker() {
            consoleThread.start();
            fileThread.start();
            dbThread.start();
        }

        public void submit(LogTask task) {

            consoleQueue.offer(task);
            fileQueue.offer(task);
            dbQueue.offer(task);
        }

        private void processConsole() {
            try {

                while (acceptingLogs || !consoleQueue.isEmpty()) {
                    LogTask task = consoleQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) consoleLogger.log(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void processFile() {
            try {
                while (acceptingLogs || !fileQueue.isEmpty()) {
                    LogTask task = fileQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) fileLogger.log(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void processDb() {
            try {
                while (acceptingLogs || !dbQueue.isEmpty()) {
                    LogTask task = dbQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) dbLogger.log(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void shutdown() {
             //stop accepting new logs.
            acceptingLogs = false;
            //Wait for all workers to finish processing
            try {
                consoleThread.join();
                fileThread.join();
                dbThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
