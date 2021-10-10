package cs437.bsu.search.engine.util;

import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

public class LoggerInitializer {

    private static LoggerInitializer INSTANCE;

    public static LoggerInitializer getInstance(){
        if(INSTANCE == null)
            INSTANCE = new LoggerInitializer();
        return INSTANCE;
    }

    private SimpleLoggerFactory slf;

    private LoggerInitializer(){
        this.slf = new SimpleLoggerFactory();
    }

    public Logger getSimpleLogger(Class<?> cls){
        return slf.getLogger(cls.getCanonicalName());
    }
}
