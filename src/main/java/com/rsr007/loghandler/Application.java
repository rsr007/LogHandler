package com.rsr007.loghandler;

import com.rsr007.loghandler.core.Logger;
import com.rsr007.loghandler.impl.CompositeLogger;

public class Application {
    private static final Logger logger =
            new CompositeLogger(Application.class);
    public static void main(String[] args) {
        logger.info("info testing");
        logger.warn("warning testing");
        logger.debug("debug testing");
        logger.error("error in testing");

        //graceful shutdown
        CompositeLogger.shutdown();
    }
}
