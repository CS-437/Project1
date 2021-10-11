package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.database.DMLCreator;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.TaskExecutor;
import edu.stanford.nlp.pipeline.CoreDocument;
import org.slf4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Document {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Document.class);

    private Map<String, Token> tokens;
    private File file;
    private String title;
    private int id;
    private boolean parsedData;

    public Document(File f){
        this.file = f;
        this.tokens = new HashMap<>();
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
                        LOGGER.trace("Found title to document. Title={},Document={}", title, id);
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
            tokens = s.getDocTokens(doc, s::removeStopwords, s::removeIllegalPatterns, s::removeLongShortTokens);

            LOGGER.trace("Token cleaning complete.");
            LOGGER.info("Tokens found in Document: {}", tokens.size());
        }, () -> {parsedData = true;});
    }

    public boolean readyToSaveData(){
        return parsedData;
    }

    public void saveData(boolean lastDoc) {
        LOGGER.debug("Adding Document to DML: {}", id);
        DMLCreator.getInstance().saveDocumentData(this, lastDoc);

        Iterator<Token> it = tokens.values().iterator();
        while (it.hasNext()) {
            Token t = it.next();
            t.saveData(id, lastDoc && !it.hasNext());
        }
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
