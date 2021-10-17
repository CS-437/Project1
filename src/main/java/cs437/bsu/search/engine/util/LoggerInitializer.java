package cs437.bsu.search.engine.util;

import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

/**
 * Utility to setup loggers. Singleton class.
 * @author Cade Peterson
 */
public class LoggerInitializer {

    private static LoggerInitializer INSTANCE;

    /**
     * Gets the Instance of this class.
     * @return Class Instance.
     */
    public static LoggerInitializer getInstance(){
        if(INSTANCE == null)
            INSTANCE = new LoggerInitializer();
        return INSTANCE;
    }

    private SimpleLoggerFactory slf;

    /** Sets up the logger factory. */
    private LoggerInitializer(){
        this.slf = new SimpleLoggerFactory();
    }

    /**
     * Loads a Logger to use.
     * @param cls Class logger is related to.
     * @return Logger associated to the class.
     */
    public Logger getSimpleLogger(Class<?> cls){
        return slf.getLogger(cls.getCanonicalName());
    }
}
