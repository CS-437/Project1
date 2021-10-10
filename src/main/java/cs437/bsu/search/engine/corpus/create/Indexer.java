package cs437.bsu.search.engine.corpus.create;

import cs437.bsu.search.engine.corpus.Document;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Indexer extends Thread {

    private static final int MAX_DOCS_BUILDING = 1000;

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Indexer.class);

    private Queue<Document> documents;
    private Saver saver;
    private boolean keepRunning;

    public Indexer(){
        this.documents = new ConcurrentLinkedQueue<>();
        this.saver = new Saver();
        keepRunning = true;
    }

    public void addDocument(File doc){
        if(keepRunning) {
            LOGGER.info("Loading Document into index: {}", doc.getAbsolutePath());
            documents.add(new Document(doc));
        }else{
            LOGGER.error("Document added after 'addedAllDocuments()' has been called: {}", doc.getAbsolutePath());
        }
    }

    public void addedAllDocuments(){
        LOGGER.info("All Documents have been added to the Indexer.");
        keepRunning = false;
    }

    @Override
    public void run() {
        LOGGER.info("Indexer running ...");
        saver.start();

        while(keepRunning || !documents.isEmpty()){
            if(!documents.isEmpty()){
                Document doc = documents.poll();
                boolean docSaved = saver.addDocument(doc);
                LOGGER.atInfo().addKeyValue("Saved", docSaved).log("Attempting to add Document to Saver: {}", doc.getDocumentPath());

                if(!docSaved)
                    documents.add(doc);
            }
        }
        saver.addedAllDocuments();

        LOGGER.info("Indexer terminating ...");
    }
}
