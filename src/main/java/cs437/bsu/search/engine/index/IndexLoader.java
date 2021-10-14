package cs437.bsu.search.engine.index;

import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.TaskExecutor;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class IndexLoader {

    private static IndexLoader INSTANCE;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(IndexLoader.class);

    public static IndexLoader getInstance(){
        if(INSTANCE == null)
            INSTANCE = new IndexLoader();
        return INSTANCE;
    }

    private Map<Integer, Document> idDocMap;
    private Map<Integer, Token> idTokenMap;
    private boolean finishedLoading;
    private long intersectionsLoaded;

    private IndexLoader(){
        idDocMap = new HashMap<>();
        idTokenMap = new HashMap<>();
        finishedLoading = false;
        intersectionsLoaded = 0;
    }

    public boolean isFinishedLoading(){
        return finishedLoading;
    }

    public void loadIndex(File dir){
        LOGGER.info("Loading index from: {}", dir.getAbsolutePath());

        Pattern sqlFilePattern = Pattern.compile("^.*\\.sql$");
        File[] files = dir.listFiles((File directory, String name) -> {
            if(directory.compareTo(dir) == 0 && !name.equalsIgnoreCase("ddl.sql")) {
                LOGGER.debug("Loading index file: {}", name);
                return sqlFilePattern.matcher(name).matches();
            }
            return false;
        });

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
            cleanup();
            LOGGER.info("Index loading complete.");
            LOGGER.info("Loaded {} Tokens, {} Documents, and {} Intersections.", idTokenMap.size(), idDocMap.size(), intersectionsLoaded);
            this.finishedLoading = true;
        });
    }

    private void cleanup(){
        LOGGER.info("Running Garbage Collector.");
        Runtime r = Runtime.getRuntime();
        r.gc();

        long maxMemory = r.maxMemory();
        long usedMemory = r.totalMemory() - r.freeMemory();
        LOGGER.warn("Using {}% JVM Memory.", String.format("%05.2f", usedMemory / (float) maxMemory));
    }

    private void loadIntersections(List<File> intersections){
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
                        case 0:
                            if(isNumeric(curr))
                                tokId += curr;
                            else
                                location++;
                            break;
                        case 1:
                            if(isNumeric(curr))
                                docId += curr;
                            else
                                location++;
                            break;
                        default:
                            if(isNumeric(curr)) {
                                freq += curr;
                            }else {
                                location++;
                                i += 10;
                            }
                            break;
                    }
                }


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

    private void loadDocuments(List<File> documents){
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
                        case 0:
                            if(isNumeric(curr))
                                id += curr;
                            else
                                location++;
                            break;
                        case 1:
                            if(isNumeric(curr)) {
                                highFreqTerm += curr;
                            }else {
                                location++;
                                i++;
                            }
                            break;
                        case 2:
                            if(curr != '"') {
                                title += curr;
                            }else{
                                location++;
                                i += 2;
                            }
                            break;
                        default:
                            if(curr != '"')
                                path += curr;
                            else
                                i += 10;
                            break;
                    }
                }

                idDocMap.put(Integer.parseInt(id), new Document(title, path, Integer.parseInt(highFreqTerm)));
                LOGGER.trace("Loading Document. ID={},Title={},Path={},HighTermFreq={}", id, title, path, highFreqTerm);
            }
        };

        for(File docFile : documents)
            readDocument(docFile, documentReader);
    }

    private void loadTokens(List<File> tokens){
        Consumer<String> tokenReader = (String line) -> {
            if(isValidEntry(line)){
                String id = "";
                String token = "";
                String hash = "";

                boolean isNumeric = true;
                char[] chars = line.toCharArray();
                for(int i = 1; i < chars.length; i++){
                    char curr = chars[i];
                    if(isNumeric) {
                        if (isNumeric(curr)) {
                            if(token.length() == 0)
                                id += curr;
                            else
                                hash += curr;
                        } else {
                            if(token.length() != 0)
                                break;

                            isNumeric = false;
                            i++;
                        }
                    }else{
                        if(curr != '"') {
                            token += curr;
                        }else{
                            isNumeric = true;
                            i++;
                        }
                    }
                }

                idTokenMap.put(Integer.parseInt(id), new Token(token, Long.parseLong(hash)));
                LOGGER.trace("Loading Token. ID={},Token={},HASH={}", id, token, hash);
            }
        };

        for(File tokFile : tokens)
            readDocument(tokFile, tokenReader);
    }

    private boolean isNumeric(char c){
        return '0' <= c && c <= '9';
    }

    private boolean isValidEntry(String s){
        return s.startsWith("(");
    }

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
