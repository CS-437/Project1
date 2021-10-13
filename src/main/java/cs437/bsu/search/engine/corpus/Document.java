package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.database.DMLCreator;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.TaskExecutor;
import edu.stanford.nlp.pipeline.CoreDocument;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;

public class Document {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Document.class);

    private Collection<Token> tokens;
    private File file;
    private String title;
    private int id;
    private boolean parsedData;

    public Document(File f){
        this.file = f;
        this.parsedData = false;

        String fileName = f.getName();
        this.id = Integer.parseInt(fileName.substring(0, fileName.indexOf('.')));
        LOGGER.info("Creating document with ID: {}", id);
    }

    public String getDocumentPath(){
        return file.getAbsolutePath();
    }

    public void parse(){
        LOGGER.debug("Starting to Parse Document: {}", id);
        TaskExecutor.StartTask(() -> {
            StringBuilder sb = new StringBuilder();

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

            Scanner s = Scanner.getInstance();

            LOGGER.trace("General document scan complete. Starting deeper scan.");
            CoreDocument doc = s.scan(sb);

            LOGGER.trace("Deeper scan complete. Starting token cleaning.");
            tokens = s.getDocTokens(doc, s::removeStopwords, s::removeNonDictionaryTerms, s::removeIllegalPatterns, s::removeLongShortTokens).values();

            LOGGER.trace("Token cleaning complete.");
            LOGGER.info("Tokens found in Document: {}", tokens.size());
        }, () -> {parsedData = true;});
    }

    public boolean readyToSaveData(){
        return parsedData;
    }

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

        if(largestFreq > 0)
            DMLCreator.getInstance().saveDocumentData(largestFreq, this, lastDoc);
    }

    public File getFile() {
        return file;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }
}
