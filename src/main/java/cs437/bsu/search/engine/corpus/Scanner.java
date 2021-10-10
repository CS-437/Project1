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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Scanner {

    private static Scanner INSTANCE;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Scanner.class);

    public synchronized static Scanner getInstance(){
        if(INSTANCE == null)
            INSTANCE = new Scanner();
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
            InputStream resource = Scanner.class.getResourceAsStream(resourceFileName);

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
    private Pattern pattern;
    private long preprocessingSize;
    private long postprocessingSize;

    private Scanner(){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        props.setProperty("tokenize.language", "English");
        props.setProperty("tokenize.options", "americanize=false");
        pipeline =  new StanfordCoreNLP(props);

        stopwords = new HashSet<>();
        for(StopwordLists sl : StopwordLists.values())
            stopwords.addAll(sl.loadStopwords());

        this.pattern = loadPatterns();

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

        String globalPattern = "";
        for(String pattern : patterns) {
            if(!globalPattern.isEmpty())
                globalPattern += "|";
            globalPattern += String.format("(%s)", pattern);
        }
        return Pattern.compile(globalPattern);
    }

    public CoreDocument scan(StringBuilder sb){
        LOGGER.info("Converting document to a CoreDocument.");
        return pipeline.processToCoreDocument(sb.toString());
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
        removeTokens(tokens,  (String token) -> {
            return stopwords.contains(token);
        });
    }

    public void removeIllegalPatterns(Map<String, Token> tokens){
        LOGGER.debug("Cleaning Tokens of illegal patterns.");
        removeTokens(tokens,  (String token) -> {
            return pattern.matcher(token).matches();
        });
    }

    private void removeTokens(Map<String, Token> tokens, Function<String, Boolean> tokenChecker){
        Iterator<Map.Entry<String, Token>> it = tokens.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, Token> entry = it.next();
            if (tokenChecker.apply(entry.getKey())) {
                LOGGER.debug("Removing useless token: {}", entry.getKey());
                it.remove();
            }
        }
    }
}
