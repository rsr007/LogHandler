# LogHandler

An asynchronous Java logging example that fans each log event out to console, file, and database handlers. It demonstrates a small logging API, immutable log events, bounded queues, and dedicated worker threads; file and database output are currently placeholders that print to standard output.

## 🏗️ Low-Level Design: Asynchronous Log Handler

### 🎯 Problem Statement

Provide a simple API for application code to emit `DEBUG`, `INFO`, `WARN`, and `ERROR` messages without performing all output work inline. Each event carries its level, message, logger name, and timestamp, and is dispatched independently to three handlers. The queues are bounded to limit memory use, and the application can stop accepting events and wait for workers to drain queued events during shutdown.

### 💎 SOLID Principles Applied

- **Single Responsibility (SRP):** `CompositeLogger` exposes the logging API and creates events; `LogWorker` queues and dispatches them; each sink class formats and emits events for one destination.
- **Open/Closed (OCP):** The `Logger` interface gives callers a stable logging contract. The current fan-out implementation has three concrete sinks; adding another sink currently requires updating `LogWorker` to add its queue, worker, and dispatch loop.
- **Interface Segregation (ISP):** `Logger` is a small caller-facing contract with only the four supported severity methods.
- **Dependency Inversion (DIP):** Callers can depend on the `Logger` abstraction rather than directly on `CompositeLogger`.

### 📐 Design Patterns Used

- **Composite:** `CompositeLogger` acts as one logger to its caller while forwarding each event to multiple output handlers. The handler set is currently wired directly in `LogWorker`, rather than represented by a common component interface.
- **Producer–consumer / asynchronous dispatch:** Logging calls produce `LogTask` events; separate worker threads consume per-sink queues so each destination is processed independently.

### 🗂️ Core Class Structure & Entities

- `Logger` — caller-facing API for the supported log levels.
- `LogLevel` — enum of `INFO`, `WARN`, `DEBUG`, and `ERROR`.
- `LogTask` — immutable record containing level, message, logger name, and `Instant` timestamp.
- `CompositeLogger` — associates a logger name with calls and submits events to the shared worker.
- `CompositeLogger.LogWorker` — owns three bounded queues and one worker thread per sink; also coordinates shutdown and queue draining.
- `ConsoleLogger`, `FileLogger`, `DbLogger` — destination-specific handlers. At present, all three print labeled output to standard output; file and database persistence are not implemented.
- `Application` — example entry point that emits a message at each supported level and shuts down logging.

### ⚡ Concurrency & Thread-Safety

- Each destination has its own `ArrayBlockingQueue<LogTask>` with capacity 1,000. `ArrayBlockingQueue` safely transfers events between application threads and the corresponding worker thread.
- Three dedicated threads poll their queues and process events independently, so a slow sink does not directly block the other sink workers.
- The shared `acceptingLogs` flag is `volatile`, so workers observe the shutdown change. Shutdown sets it to `false` and joins all three threads; workers continue until their queues are empty.
- `submit` uses non-blocking `offer` calls and currently ignores their return values. If a queue is full, that sink silently drops the event. Submission to the three queues is also not atomic, so an event can reach some sinks and be dropped by another.
- The worker is shared statically across `CompositeLogger` instances and should be shut down once, after logging is finished. Calls made after shutdown are not rejected explicitly.

## Requirements

- Java 21 or later
- Maven 3.8 or later

## Build and run

From the project root:

```sh
mvn clean package
java -cp target/classes com.rsr007.loghandler.Application
```

The sample application emits one message at each supported level: `INFO`, `WARN`, `DEBUG`, and `ERROR`.

## Use the logger

Create a `CompositeLogger` with the class that is producing the messages, then call the desired level method:

```java
import com.rsr007.loghandler.core.Logger;
import com.rsr007.loghandler.impl.CompositeLogger;

public class Service {
    private static final Logger logger = new CompositeLogger(Service.class);

    public void process() {
        logger.info("Processing started");
        logger.warn("Using fallback configuration");
        logger.debug("Detailed diagnostic information");
        logger.error("Processing failed");

        // Call once when the application is finished logging.
        CompositeLogger.shutdown();
    }
}
```

## Project layout

```text
src/main/java/com/rsr007/loghandler/
├── Application.java
├── core/       # Logger API, levels, and log event record
└── impl/       # Composite logger, worker, and output handlers
```

## Maven coordinates

- Group: `me.rsr007.loghandler`
- Artifact: `log-handler`
- Version: `1.0.0`
