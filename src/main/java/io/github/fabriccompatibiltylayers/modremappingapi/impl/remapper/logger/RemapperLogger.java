package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.logger;

import net.fabricmc.tinyremapper.api.TrLogger;
import net.legacyfabric.fabric.api.logger.v1.Logger;

public class RemapperLogger implements TrLogger {
    private final Logger logger;

    public RemapperLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(Level level, String s) {
        switch (level) {
            case ERROR:
                logger.error(s);
                break;
            case WARN:
                logger.warn(s);
                break;
            case INFO:
                logger.info(s);
                break;
            case DEBUG:
            default:
                logger.debug(s);
                break;
        }
    }
}
