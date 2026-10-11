package com.rsr007.loghandler.impl;

import com.rsr007.loghandler.core.*;

import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// Clean LLD Implementation of the Composite Logger.
public class CompositeLogger implements Logger {

    private static final LogWorker LOG_WORKER = new LogWorker();
    private final String loggerName;

    public CompositeLogger(Class<?> clazz) {
        this.loggerName = clazz.getName();
    }

    @Override
    public void debug(String message) { log(LogLevel.DEBUG, message); }
    @Override
    public void info(String message)  { log(LogLevel.INFO, message); }
    @Override
    public void warn(String message)  { log(LogLevel.WARN, message); }
    @Override
    public void error(String message) { log(LogLevel.ERROR, message); }

    private void log(LogLevel level, String message) {
        LogTask task = new LogTask(level, message, loggerName, Instant.now());
        LOG_WORKER.submit(task);
    }

    public static void shutdown() {
        System.out.println("Shutdown initialized...");
        LOG_WORKER.shutdown();
    }

    //Worker class managing target thread scopes asynchronously.
    public static class LogWorker {
        private static final int QUEUE_SIZE = 1000;

        private final BlockingQueue<LogTask> consoleQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        private final BlockingQueue<LogTask> fileQueue    = new ArrayBlockingQueue<>(QUEUE_SIZE);
        private final BlockingQueue<LogTask> dbQueue      = new ArrayBlockingQueue<>(QUEUE_SIZE);

        private final ConsoleLogger consoleLogger = new ConsoleLogger();
        private final FileLogger fileLogger       = new FileLogger();
        private final DbLogger dbLogger           = new DbLogger();

        private final Thread consoleThread;
        private final Thread fileThread;
        private final Thread dbThread;

        // for atomic update for flag set
        private final AtomicBoolean acceptingLogs = new AtomicBoolean(true);

        public LogWorker() {
            this.consoleThread = new Thread(this::processConsole, "console-log-worker");
            this.fileThread    = new Thread(this::processFile, "file-log-worker");
            this.dbThread      = new Thread(this::processDb, "db-log-worker");

            consoleThread.start();
            fileThread.start();
            dbThread.start();
        }

        public void submit(LogTask task) {
            // warn that log is not getting accepted as shutdown has begun
            if (!acceptingLogs.get()) {
                System.err.println("[REJECTED] Cannot submit logs. Logger shutdown is active: " + task.message());
                return;
            }

            // Using offer checks bounds. True tracking would alert if memory buffers fill up.
            if (!consoleQueue.offer(task)) System.err.println("[DROP] Console buffer capacity breached.");
            if (!fileQueue.offer(task))    System.err.println("[DROP] File buffer capacity breached.");
            if (!dbQueue.offer(task))      System.err.println("[DROP] Database buffer capacity breached.");
        }

        private void processConsole() {
            // while acceptingLogs and console is not empty
            while (acceptingLogs.get() || !consoleQueue.isEmpty()) {
                try {
                    LogTask task = consoleQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        consoleLogger.log(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void processFile() {
            while (acceptingLogs.get() || !fileQueue.isEmpty()) {
                try {
                    LogTask task = fileQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        fileLogger.log(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void processDb() {
            while (acceptingLogs.get() || !dbQueue.isEmpty()) {
                try {
                    LogTask task = dbQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        dbLogger.log(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public void shutdown() {
            //since acceptingLogs is Atomic, it instantly stop accept incoming logs in while loop of each log destination processing
            acceptingLogs.set(false);

            // Wait for workers to finish running logs safely
            try {
                consoleThread.join();
                fileThread.join();
                dbThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("All asynchronous logger worker threads halted cleanly.");
        }
    }
}
