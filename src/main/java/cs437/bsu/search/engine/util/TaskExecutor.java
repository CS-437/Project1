package cs437.bsu.search.engine.util;

import java.util.function.Consumer;
import java.util.function.Function;

public class TaskExecutor {

    public static <T, R> void StartTask(T value, Function<T, R> task, Consumer<R> callBack){
        Thread t = new Thread(new Task(value, task, callBack));
        t.start();
    }

    public static void StartTask(Runnable task, Runnable callBack){
        Thread t = new Thread(() -> {
            task.run();
            callBack.run();
        });
        t.start();
    }

    private static class Task<T, R> implements Runnable{

        private T value;
        private Function<T, R> task;
        private Consumer<R> callback;

        private Task(T value, Function<T, R> task, Consumer<R> callBack){

        }

        @Override
        public void run() {
            callback.accept(task.apply(value));
        }
    }
}
