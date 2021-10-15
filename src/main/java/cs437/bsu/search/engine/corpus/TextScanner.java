package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.util.LoggerInitializer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class TextScanner {

    private static TextScanner INSTANCE;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(TextScanner.class);
    private static final String DICTIONARY_RES = "dictionary.txt";

    public synchronized static TextScanner getInstance(){
        if(INSTANCE == null)
            INSTANCE = new TextScanner();
        return INSTANCE;
    }

    private enum StopwordLists{
        General         ("general-stopwords.txt"),
        More            ("more-stopwords.txt"),
        Extra           ("extra-stopwords.txt"),
        Wiki_Specific   ("wiki-specific-stopwords.txt");

        private String resourceFileName;

        private StopwordLists(String fileName){
            this.resourceFileName = fileName;
        }

        public Set<String> loadStopwords(){
            LOGGER.info("Loading stopwords from: {}", resourceFileName);

            Set<String> stopwords = new HashSet<>();
            InputStream resource = TextScanner.class.getResourceAsStream(resourceFileName);

            try(BufferedReader br = new BufferedReader(new InputStreamReader(resource))){
                String line;
                while((line = br.readLine()) != null)
                    stopwords.add(line);
            } catch (IOException e) {
                LOGGER.error("Failed to load stopwords.", e);
            }
            return stopwords;
        }
    }

    private StanfordCoreNLP pipeline;
    private Set<String> stopwords;
    private Map<Long, Set<String>> dictionary;
    private Pattern pattern;
    private long preprocessingSize;
    private long postprocessingSize;

    private TextScanner(){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        props.setProperty("tokenize.language", "English");
        props.setProperty("tokenize.options", "americanize=false");
        pipeline =  new StanfordCoreNLP(props);

        stopwords = new HashSet<>();
        for(StopwordLists sl : StopwordLists.values())
            stopwords.addAll(sl.loadStopwords());

        this.pattern = loadPatterns();
        this.dictionary = loadDictionary();

        this.preprocessingSize = 0;
        this.postprocessingSize = 0;
    }

    public long getPostProcessingSize(){
        return postprocessingSize;
    }

    public long getPreProcessingSize(){
        return preprocessingSize;
    }

    private Pattern loadPatterns(){
        LOGGER.info("Loading illegal patterns.");
        Collection<String> patterns = new ArrayList<>();
        patterns.add(Pattern.compile("\\d+(|.\\d+)").pattern()); //Numbers
        patterns.add(Pattern.compile("\\p{Punct}+").pattern()); //Symbols
        patterns.add(Pattern.compile("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]").pattern()); //URLS
        patterns.add(Pattern.compile(".*[^a-zA-Z-_`'‘]+.*").pattern()); //Only English Words

        String globalPattern = "";
        for(String pattern : patterns) {
            if(!globalPattern.isEmpty())
                globalPattern += "|";
            globalPattern += String.format("(%s)", pattern);
        }
        return Pattern.compile(globalPattern);
    }

    private Map<Long, Set<String>> loadDictionary(){
        LOGGER.info("Loading Dictionary Words.");
        Map<Long, Set<String>> dic = new HashMap<>();

        InputStream resource = TextScanner.class.getResourceAsStream(DICTIONARY_RES);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(resource))){
            String line;
            while((line = br.readLine()) != null){
                long hash = Token.getHashValue(line);
                Set<String> sameHash = dic.get(hash);
                if(sameHash == null){
                    sameHash = new HashSet<>();
                    dic.put(hash, sameHash);
                }
                sameHash.add(line);
            }
        }catch (Exception e){
            LOGGER.error("Failed to load dictionary terms.", e);
        }

        return dic;
    }

    public CoreDocument scan(StringBuilder sb){
        return scan(sb.toString());
    }

    public CoreDocument scan(String string){
        LOGGER.info("Converting document to a CoreDocument.");
        String doc = new String(string.getBytes(), StandardCharsets.US_ASCII).replace((char) 65533, '~');
        return pipeline.processToCoreDocument(doc);
    }

    public Map<String, Token> getDocTokens(CoreDocument document, Consumer<Map<String, Token>> ... cleaningMethods){
        Map<String, Token> tokens = getDocumentTokens(document);
        for(int i = 0; i < cleaningMethods.length; i++)
            cleaningMethods[i].accept(tokens);
        postprocessingSize += tokens.size();
        return tokens;
    }

    private Map<String, Token> getDocumentTokens(CoreDocument document){
        LOGGER.debug("Getting Tokens from Document.");
        Map<String, Token> tokens = new HashMap<>();
        for(CoreLabel token : document.tokens()) {
            preprocessingSize++;
            String word = token.lemma().toLowerCase();
            if(word == null)
                word = token.value().toLowerCase();

            word = word.replace(".", "");
            LOGGER.trace("Found Token: {}", word);

            Token t = tokens.get(word);
            if(t == null) {
                t = new Token(word);
                tokens.put(word, t);
                LOGGER.trace("Creating new Token for: {}", word);
            }else {
                LOGGER.trace("Increasing Token frequency for: {}", word);
                t.incrementFrequency();
            }
        }
        return tokens;
    }

    public void removeStopwords(Map<String, Token> tokens){
        LOGGER.debug("Cleaning Tokens of stopwords.");
        removeTokens(tokens,  (Token token) -> {
            return stopwords.contains(token.getToken());
        });
    }
    public void removeNonDictionaryTerms(Map<String, Token> tokens){
        LOGGER.debug("Cleaning Tokens of Non-Dictionary Terms.");
        removeTokens(tokens,  (Token token) -> {
            Set<String> hashTokens = dictionary.get(token.getHash());
            if(hashTokens == null)
                return true;
            return !hashTokens.contains(token.getToken());
        });
    }

    public void removeIllegalPatterns(Map<String, Token> tokens){
        LOGGER.debug("Cleaning Tokens of illegal patterns.");
        removeTokens(tokens,  (Token token) -> {
            return pattern.matcher(token.getToken()).matches();
        });
    }

    public void removeLongShortTokens(Map<String, Token> tokens){
        LOGGER.debug("Removing Tokens longer than 45 characters and shorter than 3.");
        removeTokens(tokens,  (Token token) -> {
            int length = token.getToken().length();
            return  length < 3 || 45 < length;
        });
    }

    private void removeTokens(Map<String, Token> tokens, Function<Token, Boolean> tokenChecker){
        Iterator<Map.Entry<String, Token>> it = tokens.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, Token> entry = it.next();
            if (tokenChecker.apply(entry.getValue())) {
                LOGGER.debug("Removing useless token: {}", entry.getKey());
                it.remove();
            }
        }
    }
}
