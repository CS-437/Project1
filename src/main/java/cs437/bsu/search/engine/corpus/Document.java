package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.database.Database;
import cs437.bsu.search.engine.database.QueryBatch;
import cs437.bsu.search.engine.database.QueryType;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.PathRelavizor;
import cs437.bsu.search.engine.util.TaskExecutor;
import edu.stanford.nlp.pipeline.CoreDocument;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

    public void saveData() {
        LOGGER.debug("Starting upload of Document data to database: {}", id);
        Database db = Database.getInstance();

        LOGGER.debug("Uploading data: id={},title={},path={}", id, title, file.getPath().toString());
        QueryBatch q = db.getQuery(QueryType.AddDocument);
        q.set(1, id);
        q.set(2, title);
        q.set(3, PathRelavizor.getRelativeLocation(file));
        q.addBatch();

        QueryBatch tokenQuery = db.getQuery(QueryType.AddToken);
        for(Token t : tokens.values())
            t.saveData(id, tokenQuery);

        if(q.getBatchSize() >= 20){
            q.executeBatch();
            tokenQuery.executeBatch();
        }
    }
}
