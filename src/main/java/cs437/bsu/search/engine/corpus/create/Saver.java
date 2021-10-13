package cs437.bsu.search.engine.corpus.create;

import cs437.bsu.search.engine.corpus.Document;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Saver extends Thread {

    private static final int NUM_OPEN_PARSING_DOCS = 100;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Saver.class);

    private Queue<Document> documents;
    private boolean keepRunning;

    public Saver(){
        this.documents = new ConcurrentLinkedQueue<>();
        this.keepRunning = true;
    }

    public boolean addDocument(Document doc){
        if(keepRunning) {
            if (documents.size() < NUM_OPEN_PARSING_DOCS) {
                doc.parse();
                documents.add(doc);
                return true;
            }else{
                LOGGER.debug("Unable to add Document as Queue is full: {}", doc.getDocumentPath());
            }
        }else{
            LOGGER.error("Document added after 'addedAllDocuments()' has been called: {}", doc.getDocumentPath());
        }
        return false;
    }

    public void addedAllDocuments() {
        LOGGER.info("All Documents have been added to the Saver.");
        keepRunning = false;
    }

    @Override
    public void run() {
        LOGGER.info("Saver running ...");

        while(keepRunning || !documents.isEmpty()){
            while(!documents.isEmpty()){
                Document doc = documents.poll();
                if(doc.readyToSaveData()) {
                    doc.saveData(!keepRunning && documents.isEmpty());
                    LOGGER.debug("Attempting to load Document data into database: {}", doc.getDocumentPath());
                }else {
                    documents.add(doc);
                }
            }
        }

        LOGGER.info("Saver terminating ...");
    }
}
