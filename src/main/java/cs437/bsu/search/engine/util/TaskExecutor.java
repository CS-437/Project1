package cs437.bsu.search.engine.util;

import org.slf4j.Logger;

/**
 * Utility Class used to start threaded tasks or dealing with threads.
 * @author Cade Peterson
 */
public class TaskExecutor {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(TaskExecutor.class);
    private static long TASK_ID = 0;

    /**
     * Starts a Tasks with a Call Back and kicks off this task as a thread.
     * Note that both parameters are treated as a function and not a runnable
     * threaded function. Both are placed in the same thread.
     * @param task Task to preform.
     * @param callBack Call back to execute once task is done.
     */
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

    /**
     * Makes the current thread invoking
     * this method to sleep x milliseconds.
     * @param milliseconds Amount of time to sleep for.
     */
    public static void sleep(long milliseconds){
        try{
            Thread.sleep(milliseconds);
        }catch (Exception e){
            LOGGER.atWarn().setCause(e).log("Failed to wait: {}", milliseconds);
        }
    }
}
