package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.index.IndexCreator;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.TaskExecutor;
import edu.stanford.nlp.pipeline.CoreDocument;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Represents a Document during the processing of a corpus.
 * Holds information such as the documents title, tokens found within,
 * file location, etc.
 * @author Cade Peterson
 */
public class Document {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Document.class);

    private Collection<Token> tokens;
    private File file;
    private String title;
    private int id;
    private boolean parsedData;

    /**
     * Creates a Document and sets things up for scanning.
     * @param f File of the document to scan.
     */
    public Document(File f){
        this.file = f;
        this.parsedData = false;

        String fileName = f.getName();
        this.id = Integer.parseInt(fileName.substring(0, fileName.indexOf('.')));
        LOGGER.info("Creating document with ID: {}", id);
    }

    /**
     * Returns the path of the document file.
     * @return Absolute file path.
     */
    public String getDocumentPath(){
        return file.getAbsolutePath();
    }

    /**
     * Starts the process of scanning the document. This method kicks off
     * a thread and returns once the thread has been started. In order
     * to know when the parsing is done invoke {@link #readyToSaveData()}.
     */
    public void parse(){
        LOGGER.debug("Starting to Parse Document: {}", id);
        TaskExecutor.StartTask(() -> {
            StringBuilder sb = new StringBuilder();

            // Gathers the document title and body contents
            try(BufferedReader br = new BufferedReader(new FileReader(file))){
                String line;
                for(int i = 0; (line = br.readLine()) != null; i++){
                    if(i == 0) {
                        title = line.substring(7);
                        if(title.length() > 120)
                            title = title.substring(0, 116) + " ...";

                        LOGGER.trace("Found title to document. Title={},Document={}", title, id);
                        sb.append(line + " ");
                    }else if(i > 1) {
                        sb.append(line + " ");
                    }
                }
            } catch (IOException e) {
                LOGGER.atError().setCause(e).log("Failed to parse Document fully: {}", getDocumentPath());
            }

            TextScanner s = TextScanner.getInstance();

            LOGGER.trace("General document scan complete. Starting deeper scan.");
            CoreDocument doc = s.scan(sb);

            LOGGER.trace("Deeper scan complete. Starting token cleaning.");
            tokens = s.getDocTokens(doc, s::removeStopwords, s::removeNonDictionaryTerms, s::removeIllegalPatterns, s::removeLongShortTokens).values();

            LOGGER.trace("Token cleaning complete.");
            LOGGER.info("Tokens found in Document: {}", tokens.size());
        }, () -> {parsedData = true;});
    }

    /**
     * Dictates if the document has parsed or finished parsing its file for tokens.
     * @return true if document has been parsed, otherwise false.
     */
    public boolean readyToSaveData(){
        return parsedData;
    }

    /**
     * Saves the document and its token info to a file.
     * @param lastDoc True if this is the last document to save.
     */
    public void saveData(boolean lastDoc) {
        LOGGER.debug("Adding Document to DML: {}", id);

        int largestFreq = 0;
        Iterator<Token> it = tokens.iterator();
        while (it.hasNext()) {
            Token t = it.next();
            if(t.getFrequency() > largestFreq)
                largestFreq = t.getFrequency();
            t.saveData(id, lastDoc && !it.hasNext());
        }

        // No need to save document as it has not valid tokens
        if(largestFreq > 0)
            IndexCreator.getInstance().saveDocumentData(largestFreq, this, lastDoc);
    }

    /**
     * Gets the documents file.
     * @return Document file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Gets the title of the document.
     * @return Title of document.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the id of the document.
     * @return Document id.
     */
    public int getId() {
        return id;
    }
}
