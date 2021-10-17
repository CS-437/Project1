package cs437.bsu.search.engine.corpus.create;

import cs437.bsu.search.engine.corpus.Document;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Used to index documents that it is provided. Note that when started it will
 * use a {@link Saver} to add documents to. This class won't start a document
 * for processing but through interactions with the Saver it will.
 * @author Cade Peterson
 */
public class Indexer extends Thread {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Indexer.class);

    private Queue<Document> documents;
    private Saver saver;
    private boolean keepRunning;

    /**
     * Creates an Index that documents can be added to while it's running.
     */
    public Indexer(){
        this.documents = new ConcurrentLinkedQueue<>();
        this.saver = new Saver();
        keepRunning = true;
    }

    /**
     * Adds a document to the Indexer. Works while it's running too.
     * Note that if any call to this method is made after {@link #addedAllDocuments()}
     * the document won't be added.
     * @param doc Document to add to the Index.
     */
    public void addDocument(File doc){
        if(keepRunning) {
            LOGGER.info("Loading Document into index: {}", doc.getAbsolutePath());
            documents.add(new Document(doc));
        }else{
            LOGGER.error("Document added after 'addedAllDocuments()' has been called: {}", doc.getAbsolutePath());
        }
    }

    /**
     * This method indicates to the indexer that no more documents will be added to
     * it and that all documents have been added that are wanted.
     */
    public void addedAllDocuments(){
        LOGGER.info("All Documents have been added to the Indexer.");
        keepRunning = false;
    }

    @Override
    public void run() {
        LOGGER.info("Indexer running ...");
        saver.start();

        // Loop until no more documents are found and until
        // this class is signaled that no more will be added
        while(keepRunning || !documents.isEmpty()){
            if(!documents.isEmpty()){

                // Remove document and add to saver
                // If unable add back to queue
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
