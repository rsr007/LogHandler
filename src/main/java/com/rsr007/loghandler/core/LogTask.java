package com.rsr007.loghandler.core;

import java.time.Instant;

public record LogTask(LogLevel level,
                      String message,
                      String loggerName,
                      Instant timestamp) {
}
