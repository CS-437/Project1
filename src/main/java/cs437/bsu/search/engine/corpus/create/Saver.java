package cs437.bsu.search.engine.corpus.create;

import cs437.bsu.search.engine.corpus.Document;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Saver extends Thread {

    private static final int NUM_OPEN_PARSING_DOCS = 1000;
    private static final Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Saver.class);

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

        pause(4000);

        while(keepRunning || !documents.isEmpty()){
            while(!documents.isEmpty()){
                Document doc = documents.poll();
                boolean readyToSave = doc.readyToSaveData();
                LOGGER.atTrace().addKeyValue("Loading", readyToSave).log("Attempting to load Document data into database: {}", doc.getDocumentPath());

                if(readyToSave)
                    doc.saveData();
                else
                    documents.add(doc);
            }
            pause(50);
        }

        LOGGER.info("Saver terminating ...");
    }

    private void pause(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            LOGGER.warn("Failed to sleep for 50 milliseconds.", e);
        }
    }
}
