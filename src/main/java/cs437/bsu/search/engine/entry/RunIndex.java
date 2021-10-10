package cs437.bsu.search.engine.entry;

import cs437.bsu.search.engine.corpus.Scanner;
import cs437.bsu.search.engine.corpus.create.Indexer;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.io.*;

public class RunIndex {

    private static final Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(RunIndex.class);
    private static Indexer indexer;

    public static void main(String[] args) {
        if(args.length > 1){
            String msg = "Only expecting one arg, the directory to scan to create the index.";
            System.err.println(msg);
            LOGGER.warn(msg);
        }

        if(args.length == 1) {
            LOGGER.trace("Starting Index Creation.");
            createIndex(args[0]);
        }else {
            LOGGER.trace("Starting Search Engine.");
            searchEngine();
        }
    }

    private static void createIndex(String directory){
        File dir = new File(directory);
        if(dir.exists() && dir.isDirectory()){
            LOGGER.info("Indexing directory: {}", dir.getAbsolutePath());

            File[] files = dir.listFiles();
            indexer = new Indexer();
            indexer.start();

            for(File f : files)
                indexer.addDocument(f);

            indexer.addedAllDocuments();
            LOGGER.info("Finished loading all documents to be indexed.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Scanner s = Scanner.getInstance();
                System.out.printf("Total Tokens found Pre-Processing: %d%n", s.getPreProcessingSize());
                System.out.printf("Total Tokens found Post-Processing: %d%n", s.getPostProcessingSize());
            }));
        }else{
            String msg = String.format("The directory provided doesn't exist and/or wasn't a directory. (%s)", directory);
            LOGGER.trace(msg);
            System.err.println(msg);
        }
    }

    private static void searchEngine(){
    }
}
