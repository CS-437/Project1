package cs437.bsu.search.engine.util;

import org.slf4j.Logger;

public class TaskExecutor {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(TaskExecutor.class);
    private static long TASK_ID = 0;

    public synchronized static void StartTask(Runnable task, Runnable callBack){
        long id = TASK_ID++;
        Thread t = new Thread(() -> {
            LOGGER.trace("Starting Task: {}", id);
            task.run();
            LOGGER.trace("Invoking callback for Task: {}", id);
            callBack.run();
            LOGGER.trace("Ending Task: {}", id);
        });
        t.start();
    }
}
