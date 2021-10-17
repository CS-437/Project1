package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.util.LoggerInitializer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
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

/**
 * Singleton class used to Scan a piece of text. This text could be anything
 * from a Document to a Sentence.
 * @author Cade Peterson
 */
public class TextScanner {

    private static TextScanner INSTANCE;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(TextScanner.class);
    private static final String DICTIONARY_RES = "dictionary.txt";

    /**
     * Gets the Singleton instance.
     * @return Singleton instance.
     */
    public synchronized static TextScanner getInstance(){
        if(INSTANCE == null)
            INSTANCE = new TextScanner();
        return INSTANCE;
    }

    /**
     * Various Stop word lists that are available.
     */
    private enum StopwordLists{
        /** Very Common stopwords **/
        General         ("general-stopwords.txt"),
        /** Common stopwords **/
        More            ("more-stopwords.txt"),
        /** Extra stopwords **/
        Extra           ("extra-stopwords.txt"),
        /** Wiki specific stopwords **/
        Wiki_Specific   ("wiki-specific-stopwords.txt");

        private String resourceFileName;

        /**
         * Constructs an enum with the stopword resource name.
         * @param fileName resource name found in jar artifact.
         */
        private StopwordLists(String fileName){
            this.resourceFileName = fileName;
        }

        /**
         * Loads the stopwords from this enums resource pointer.
         * @return Set of stopwords.
         */
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

    /**
     * Creates a text scanner.
     * When created the following items are loaded and saved:
     * <ul>
     *     <li>CoreNLP Pipeline - Loaded wit a tokenizer, sentence splitter, part of speech, and lemmatizer.
     *     <li>All stopwords from {@link StopwordLists}
     *     <li>Patterns to find things such as numbers, urls, things that aren't really token.
     *     <li>Dictionary of legal english words.
     * </ul>
     */
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

    /**
     * Get the number of tokens found post-processing. Note
     * that this number doesn't represent unique tokens.
     * Duplicates are counted as well.
     * @return Number of tokens found post-processing.
     */
    public long getPostProcessingSize(){
        return postprocessingSize;
    }

    /**
     * Get the number of tokens found pre-processing. Note
     * that this number doesn't represent unique tokens.
     * Duplicates are counted as well.
     * @return Number of tokens found pre-processing.
     */
    public long getPreProcessingSize(){
        return preprocessingSize;
    }

    /**
     * Loads the various patterns into one pattern.
     * The various patterns are loaded:
     * <ul>
     *     <li>A Number Pattern to find numbers anywhere in a token.
     *     <li>A Punctuation pattern to find symbols anywhere in a token.
     *     <li>A URL pattern to find urls anywhere in a token.
     *     <li>A Non English Letter pattern to find tokens that contain letters that aren't valid english letters.
     * </ul>
     * @return Singular pattern to match anything from the loaded patterns.
     */
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

    /**
     * Loads a Dictionary from a resource within the artifact.
     * @return A map of hash-values to a set of tokens for quicker look up.
     */
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

    /**
     * Scans a String Builder into a CoreNLP Document.
     * @param sb StringBuilder to scan.
     * @return Created CoreNLP Document.
     * @see #scan(String)
     */
    public CoreDocument scan(StringBuilder sb){
        return scan(sb.toString());
    }

    /**
     * Scans a String into a CoreNLP Document. Note that the String is converted into the
     * ASCII standard Charset and all characters (65533) are converted to tilda's.
     * @param string String to scan.
     * @return Created CoreNLP Document.
     */
    public CoreDocument scan(String string){
        LOGGER.info("Converting document to a CoreDocument.");
        String doc = new String(string.getBytes(), StandardCharsets.US_ASCII).replace((char) 65533, '~');
        return pipeline.processToCoreDocument(doc);
    }

    /**
     * Scans a Document and retrieves all viable tokens found.
     * @param document Document to scan.
     * @param cleaningMethods Methods to apply to each token to test for viability.
     * @return Map of token String to actual Token object.
     */
    public Map<String, Token> getDocTokens(CoreDocument document, Consumer<Map<String, Token>> ... cleaningMethods){
        Map<String, Token> tokens = getTokens(document.tokens());
        for(int i = 0; i < cleaningMethods.length; i++)
            cleaningMethods[i].accept(tokens);
        postprocessingSize += tokens.size();
        return tokens;
    }

    /**
     * Scans a Sentence and retrieves all viable tokens found.
     * @param sentence Sentence to scan.
     * @param cleaningMethods Methods to apply to each token to test for viability.
     * @return Map of token String to actual Token object.
     */
    public Map<String, Token> getSentenceTokens(CoreSentence sentence, Consumer<Map<String, Token>> ... cleaningMethods){
        Map<String, Token> tokens = getTokens(sentence.tokens());
        for(int i = 0; i < cleaningMethods.length; i++)
            cleaningMethods[i].accept(tokens);
        postprocessingSize += tokens.size();
        return tokens;
    }

    /**
     * Scans a list of CoreNLP Tokens and turns them into a map of Unique Tokens.
     * If duplicates are found the {@link Token#getFrequency() frequnecy} of the
     * token is incremented. Also all tokens are lemmatized if possible.
     * @param labels CoreNLP Tokens to scan.
     * @return Map of token strings to Token objects.
     */
    private Map<String, Token> getTokens(List<CoreLabel> labels){
        LOGGER.debug("Getting Tokens from Document/Sentence.");
        Map<String, Token> tokens = new HashMap<>();
        for(CoreLabel token : labels) {
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

    /**
     * Function to remove stop words from a map of tokens.
     * @param tokens Tokens to clean of stop words.
     */
    public void removeStopwords(Map<String, Token> tokens){
        LOGGER.debug("Cleaning Tokens of stopwords.");
        removeTokens(tokens,  (Token token) -> {
            return stopwords.contains(token.getToken());
        });
    }

    /**
     * Function to remove words that aren't found in a dictionary.
     * @param tokens Tokens to clean of non dictionary words.
     */
    public void removeNonDictionaryTerms(Map<String, Token> tokens){
        LOGGER.debug("Cleaning Tokens of Non-Dictionary Terms.");
        removeTokens(tokens,  (Token token) -> {
            Set<String> hashTokens = dictionary.get(token.getHash());
            if(hashTokens == null)
                return true;
            return !hashTokens.contains(token.getToken());
        });
    }

    /**
     * Function to remove tokens that match illegal patterns.
     * @param tokens Tokens to clean of illegal patterns.
     */
    public void removeIllegalPatterns(Map<String, Token> tokens){
        LOGGER.debug("Cleaning Tokens of illegal patterns.");
        removeTokens(tokens,  (Token token) -> {
            return pattern.matcher(token.getToken()).matches();
        });
    }

    /**
     * Function to remove tokens that are too long or short.
     * If tokens are found to be between 3-45 characters in length
     * thet are kept otherwise they are removed.
     * @param tokens Tokens to clean due to their length.
     */
    public void removeLongShortTokens(Map<String, Token> tokens){
        LOGGER.debug("Removing Tokens longer than 45 characters and shorter than 3.");
        removeTokens(tokens,  (Token token) -> {
            int length = token.getToken().length();
            return  length < 3 || 45 < length;
        });
    }

    /**
     * Method for looping over tokens and applying the checker.
     * @param tokens Tokens to scan through.
     * @param tokenChecker Token check to use on each token. If true is returned from this
     *                     method the token is removed, otherwise it is kept.
     */
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
