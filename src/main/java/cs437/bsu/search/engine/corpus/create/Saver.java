package cs437.bsu.search.engine.corpus.create;

import cs437.bsu.search.engine.corpus.Document;
import cs437.bsu.search.engine.entry.Run;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class works in tandem with the {@link Indexer}. This class will
 * take documents from the Indexer and start scanning them. While these
 * new documents are being scanned any documents that are done are removed
 * and saved. This class also puts a throttle on how many documents can
 * be parsed synchronously at any given moment.
 * @author Cade Peterson
 */
public class Saver extends Thread {

    private static final int NUM_OPEN_PARSING_DOCS = 100;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Saver.class);

    private Queue<Document> documents;
    private boolean keepRunning;

    /**
     * Creates a Saver.
     */
    public Saver(){
        this.documents = new ConcurrentLinkedQueue<>();
        this.keepRunning = true;
    }

    /**
     * Adds a Document to the Saver if room is allowed. If allowed to be
     * added document is processed here. Note that if any call to this
     * method is made after {@link #addedAllDocuments()} the document
     * won't be added.
     * @param doc Document to add to the Saver.
     * @return true if the document was added, otherwise false.
     */
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

    /**
     * This method indicates to the Saver that no more documents will be added to
     * it and that all documents have been added that are wanted.
     */
    public void addedAllDocuments() {
        LOGGER.info("All Documents have been added to the Saver.");
        keepRunning = false;
    }

    @Override
    public void run() {
        LOGGER.info("Saver running ...");

        // Loop until no more documents are found and until
        // this class is signaled that no more will be added
        while(keepRunning || !documents.isEmpty()){
            while(!documents.isEmpty()){

                // Currently, this is single threaded.
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
        Run.appDone = true;
    }
}
