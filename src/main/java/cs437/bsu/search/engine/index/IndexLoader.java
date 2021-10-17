package cs437.bsu.search.engine.index;

import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.TaskExecutor;
import cs437.bsu.search.engine.util.Text;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Loads a Reverse Index and provides information regarding it.
 * This class is a Singleton.
 * @author Cade Peterson, Nick Dieghton
 */
public class IndexLoader {

    private static IndexLoader INSTANCE;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(IndexLoader.class);

    /**
     * Gets this classes instance.
     * @return Class Instance.
     */
    public static IndexLoader getInstance(){
        if(INSTANCE == null)
            INSTANCE = new IndexLoader();
        return INSTANCE;
    }

    private Map<Integer, Doc> idDocMap;
    private Map<Integer, Term> idTokenMap;
    private Map<Long, Map<String, Term>> hashTokenMap;
    private boolean finishedLoading;
    private long intersectionsLoaded;

    /** Sets up the Index Loader. */
    private IndexLoader(){
        idDocMap = new HashMap<>();
        idTokenMap = new HashMap<>();
        hashTokenMap = new HashMap<>();
        finishedLoading = false;
        intersectionsLoaded = 0;
    }

    /**
     * Dictates if this Index Loader has Finished Loading
     * both the Index and AOL Query Logs.
     * @return True if finished otherwise false.
     * @see #loadIndex(File)
     * @see #loadQueryLogs(File)
     */
    public boolean isFinishedLoading(){
        return finishedLoading;
    }

    /**
     * Given a Document ID the Real Document
     * is retrieved matching it.
     * @param id ID of the document to retrieve.
     * @return Document associated to the ID or null if none.
     */
    public Doc getDocById(int id){
        return idDocMap.get(id);
    }

    /**
     * Gets the Total number of Documents loaded.
     * @return Documents Loaded count.
     */
    public int getNumDocs(){
        return idDocMap.size();
    }

    /**
     * Given a Term Hash-Value and String the matching token
     * is found.
     * @param hash Hash value of the String.
     * @param s String form.
     * @return Term associated to the hash and String or null if none.
     */
    public Term getTermByHashToken(long hash, String s){
        Map<String, Term> sameHashValues = hashTokenMap.get(hash);
        if(sameHashValues != null)
            return sameHashValues.get(s);
        return null;
    }

    /**
     * Loads the Index from the directory provided. Only files ending in
     * .SQL are loaded except for DDL files. Note that once this method
     * is invoked a Thread will be kicked off to load the index. Refer
     * to {@link #isFinishedLoading()} to know when this is thread has
     * completed.
     * @param dir Directory to load Index from.
     */
    public void loadIndex(File dir){
        LOGGER.info("Loading index from: {}", dir.getAbsolutePath());

        // Gets required files
        Pattern sqlFilePattern = Pattern.compile("^.*\\.sql$");
        File[] files = dir.listFiles((File directory, String name) -> {
            if(directory.compareTo(dir) == 0 && !name.equalsIgnoreCase("ddl.sql")) {
                LOGGER.debug("Loading index file: {}", name);
                return sqlFilePattern.matcher(name).matches();
            }
            return false;
        });

        // Split files into matching groups
        List<File> documents = new ArrayList<>();
        List<File> tokens = new ArrayList<>();
        List<File> intersections = new ArrayList<>();
        for(File file : files){
            if(file.getName().contains("intersection"))
                intersections.add(file);
            else if(file.getName().contains("tokens"))
                tokens.add(file);
            else
                documents.add(file);
        }

        LOGGER.debug("Found {} intersection file(s), {} token file(s), and {} document file(s).", intersections.size(), tokens.size(), documents.size());

        // Start a task to load the files into the program.
        TaskExecutor.StartTask(() -> {
            LOGGER.debug("Starting to load Tokens and Documents simultaneously.");
            Thread tok = new Thread(() -> loadTokens(tokens));
            Thread doc = new Thread(() -> loadDocuments(documents));
            tok.start();
            doc.start();

            while(tok.getState() != Thread.State.TERMINATED || doc.getState() != Thread.State.TERMINATED){
                try{
                    Thread.sleep(1000);
                }catch (Exception e){}
            }
            LOGGER.trace("Tokens and Document loaded.");
            cleanup();
        }, () -> {
            LOGGER.info("Starting to load intersections.");
            loadIntersections(intersections);
            long tokensLoaded = idTokenMap.size();

            transitionTokenMap();
            cleanup();

            LOGGER.info("Index loading complete.");
            LOGGER.info("Loaded {} Tokens, {} Documents, and {} Intersections.", tokensLoaded, idDocMap.size(), intersectionsLoaded);
            this.finishedLoading = true;
        });
    }
//    /**
//     * Loads the AOL Query logs the directory provided. All files ending in .txt
//     * are loaded and considered AOL Query Logs. Note that once this method is
//     * invoked a Thread will be kicked off to load the index. Refer to
//     * {@link #isFinishedLoading()} to know when this is thread has completed.
//     * @param aolDir Directory to load AOL Query Logs from.
//     */
//    public void loadQueryLogs (File aolDir) {
//
//        LOGGER.info("Loading query logs from: {}", aolDir.getAbsolutePath());
//
////        Pattern sqlFilePattern = Pattern.compile("^.*\\.sql$");
////        File[] files = aolDir.listFiles((File directory, String name) -> {
////
////        });
//    }

    /**
     * Transitions all tokens from the {@link #idTokenMap} to the
     * {@link #hashTokenMap} for post index usage. Note that the
     * idTokenMap will be set to null to lower the amount of memory
     * consumed.
     */
    private void transitionTokenMap(){
        Iterator<Map.Entry<Integer, Term>> it = idTokenMap.entrySet().iterator();
        while(it.hasNext()){
            Term token = it.next().getValue();
            it.remove();

            Map<String, Term> tokens = hashTokenMap.get(token.getHashValue());
            if(tokens == null){
                tokens = new HashMap<>();
                hashTokenMap.put(token.getHashValue(), tokens);
            }
            tokens.put(token.getToken(), token);
        }
        idTokenMap = null;
    }

    /**
     * Runs the Garbage Collector and logs the
     * current used memory consumption.
     */
    private void cleanup(){
        LOGGER.info("Running Garbage Collector.");
        Runtime r = Runtime.getRuntime();
        r.gc();

        long maxMemory = r.maxMemory();
        long usedMemory = r.totalMemory() - r.freeMemory();
        LOGGER.warn("Using {}% JVM Memory.", String.format("%05.2f", usedMemory / (float) maxMemory));
    }

    /**
     * Loads all the Intersection files.
     * @param intersections Files to load related to intersection
     *                     between Tokens and Documents.
     */
    private void loadIntersections(List<File> intersections){
        // Function to process each line of the file and
        // save data if it's a data line
        Consumer<String> intersectionReader = (String line) -> {
            if(isValidEntry(line)){
                String tokId = "";
                String docId = "";
                String freq = "";

                byte location = 0;
                char[] chars = line.toCharArray();
                for(int i = 1; i < chars.length; i++){
                    char curr = chars[i];
                    switch (location){
                        case 0: // Process Token ID
                            if(Text.isNumeric(curr))
                                tokId += curr;
                            else
                                location++;
                            break;
                        case 1: // Process Document ID
                            if(Text.isNumeric(curr))
                                docId += curr;
                            else
                                location++;
                            break;
                        default: // Process Token Frequency
                            if(Text.isNumeric(curr)) {
                                freq += curr;
                            }else {
                                location++;
                                i += 10;
                            }
                            break;
                    }
                }

                // Catch in case no matching document or token has been found previously.
                try {
                    idTokenMap.get(Integer.parseInt(tokId)).addDocumentLink(Integer.parseInt(docId), Integer.parseInt(freq));
                }catch (NullPointerException e){
                    LOGGER.atError().setCause(e).log("Failed to load data. TokenID={},DocumentToken={},Freq={}", tokId, docId, freq);
                    throw e;
                }
                LOGGER.trace("Loading Intersection. TokenID={},DocumentToken={},Freq={}", tokId, docId, freq);
                intersectionsLoaded++;
            }
        };

        for(File interFile : intersections)
            readDocument(interFile, intersectionReader);
    }

    /**
     * Loads all Document Files.
     * @param documents Documents to load.
     */
    private void loadDocuments(List<File> documents){
        // Function to process each line of the file and
        // save data if it's a data line
        Consumer<String> documentReader = (String line) -> {
            if(isValidEntry(line)){
                String id = "";
                String highFreqTerm = "";
                String title = "";
                String path = "";

                byte location = 0;
                char[] chars = line.toCharArray();
                for(int i = 1; i < chars.length; i++){
                    char curr = chars[i];
                    switch (location){
                        case 0: // Process Document ID
                            if(Text.isNumeric(curr))
                                id += curr;
                            else
                                location++;
                            break;
                        case 1: // Process highest Term Count in Document
                            if(Text.isNumeric(curr)) {
                                highFreqTerm += curr;
                            }else {
                                location++;
                                i++;
                            }
                            break;
                        case 2: // Process Document Title
                            if(curr != '"') {
                                title += curr;
                            }else{
                                location++;
                                i += 2;
                            }
                            break;
                        default: // Process Document Path
                            if(curr != '"')
                                path += curr;
                            else
                                i += 10;
                            break;
                    }
                }

                int docId = Integer.parseInt(id);
                idDocMap.put(docId, new Doc(docId, title, path, Integer.parseInt(highFreqTerm)));
                LOGGER.trace("Loading Document. ID={},Title={},Path={},HighTermFreq={}", id, title, path, highFreqTerm);
            }
        };

        for(File docFile : documents)
            readDocument(docFile, documentReader);
    }

    /**
     * Loads all Token Files.
     * @param tokens Tokens to load.
     */
    private void loadTokens(List<File> tokens){
        // Function to process each line of the file and
        // save data if it's a data line
        Consumer<String> tokenReader = (String line) -> {
            if(isValidEntry(line)){
                String id = "";
                String token = "";
                String hash = "";

                boolean isNumeric = true;
                char[] chars = line.toCharArray();
                for(int i = 1; i < chars.length; i++){
                    char curr = chars[i];
                    if(isNumeric) { // Process Numeric Data
                        if (Text.isNumeric(curr)) {
                            if(token.length() == 0) // Process ID
                                id += curr;
                            else // Process Hash-Value
                                hash += curr;
                        } else {
                            if(token.length() != 0)
                                break;

                            isNumeric = false;
                            i++;
                        }
                    }else{ // Process Token String Value
                        if(curr != '"') {
                            token += curr;
                        }else{
                            isNumeric = true;
                            i++;
                        }
                    }
                }

                idTokenMap.put(Integer.parseInt(id), new Term(token, Long.parseLong(hash)));
                LOGGER.trace("Loading Token. ID={},Token={},HASH={}", id, token, hash);
            }
        };

        for(File tokFile : tokens)
            readDocument(tokFile, tokenReader);
    }

    /**
     * Dictates if the current String is a Data String or replace string.
     * @param s String to check if it should be parsed for its data.
     * @return true if parsable, otherwise false.
     */
    private boolean isValidEntry(String s){
        return s.startsWith("(");
    }

    /**
     * Reads a File for its data and loads it into the Index Loader.
     * @param f File to parse line by line.
     * @param entryDecider Function to parse each line and save its data as needed.
     */
    private void readDocument(File f, Consumer<String> entryDecider){
        LOGGER.debug("Reading File: {}", f);
        long currLine = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String line;
            while((line = br.readLine()) != null) {
                currLine++;
                entryDecider.accept(line);
            }
        }catch (Exception e){
            LOGGER.atError().setCause(e).log("Failed to read from file correctly at line {}: {}", currLine, f);
        }
    }
}
