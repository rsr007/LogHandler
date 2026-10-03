# LogHandler

LogHandler is a small asynchronous Java logging example built around the
Composite design pattern. A log call is packaged with its level, message,
logger name, and timestamp, then dispatched to console, file, and database
handlers by separate worker threads.

## Requirements

- Java 21 or later
- Maven 3.8 or later

## Build and run

From the project root:

```sh
mvn clean package
java -cp target/classes com.rsr007.loghandler.Application
```

The sample application emits one message at each supported level: `INFO`,
`WARN`, `DEBUG`, and `ERROR`. Each handler currently writes a labeled line to
standard output. `FileLogger` and `DbLogger` are placeholders; they do not yet
write to a file or connect to a database.

## Use the logger

Create a `CompositeLogger` with the class that is producing the messages, then
call the desired level method:

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

The logger submits each message to three bounded queues (capacity 1,000 each).
Dedicated workers consume those queues asynchronously. Call
`CompositeLogger.shutdown()` when logging is complete so workers drain pending
messages and stop. The current worker is shared statically and is intended to
be shut down once, at application shutdown.

## Project layout

```text
src/main/java/com/rsr007/loghandler/
├── Application.java
├── core/       # Logger API, levels, and log event record
└── impl/       # Composite logger and output handlers
```

## Maven coordinates

- Group: `me.rsr007.loghandler`
- Artifact: `log-handler`
- Version: `1.0.0`
