package cs437.bsu.search.engine.entry;

import cs437.bsu.search.engine.corpus.TextScanner;
import cs437.bsu.search.engine.corpus.create.Indexer;
import cs437.bsu.search.engine.index.IndexLoader;
import cs437.bsu.search.engine.query.SearchEngine;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.TaskExecutor;
import org.slf4j.Logger;

import java.io.*;

public class Run {

    private static Logger LOGGER;
    private static Indexer indexer;

    public static void main(String[] args) {
        ArgumentParser ap = new ArgumentParser(args);
        LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Run.class);

        switch (ArgumentParser.application) {
            case CreateIndex:
                createIndex(ap.getDirectory());
                break;
            default:
                searchEngine(ap.getDirectory());
                break;
        }
    }

    private static void createIndex(File indexDirectory){
        LOGGER.info("Starting Index Creation ...");
        LOGGER.info("Indexing directory: {}", indexDirectory.getAbsolutePath());


        long start = System.currentTimeMillis();
        File[] files = indexDirectory.listFiles();
        indexer = new Indexer();
        indexer.start();

        for(File f : files)
            indexer.addDocument(f);

        indexer.addedAllDocuments();
        LOGGER.info("Finished loading all documents to be indexed.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TextScanner s = TextScanner.getInstance();
            System.out.printf("Total Tokens found Pre-Processing: %d%n", s.getPreProcessingSize());
            System.out.printf("Total Tokens found Post-Processing: %d%n", s.getPostProcessingSize());

            long duration = System.currentTimeMillis() - start;
            System.out.printf("Index Creation Duration: %s%n", getTimeLength(duration));
        }));
    }

    private static void searchEngine(File dir){
        LOGGER.info("Starting Search Engine ...");
        IndexLoader il = IndexLoader.getInstance();
        il.loadIndex(dir);

        System.out.print("Loading Index ");
        while(!il.isFinishedLoading()){
            for(int i = 0; i < 3; i++){
                System.out.print(".");
                TaskExecutor.sleep(750);
            }
            System.out.print("\b\b\b");
            TaskExecutor.sleep(750);
        }
        System.out.println();

        LOGGER.info("Index loaded. Starting Search Engine.");
        new SearchEngine().start();
    }

    private static String getTimeLength(long duration){
        long millis = duration % 1000;
        long second = (duration / 1000) % 60;
        long minute = (duration / (1000 * 60)) % 60;
        long hour = (duration / (1000 * 60 * 60)) % 24;
        return String.format("%02d:%02d:%02d.%d", hour, minute, second, millis);
    }
}
