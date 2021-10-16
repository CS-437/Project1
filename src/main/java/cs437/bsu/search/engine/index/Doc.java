package cs437.bsu.search.engine.index;

import cs437.bsu.search.engine.container.Pair;
import cs437.bsu.search.engine.container.Triple;
import cs437.bsu.search.engine.corpus.TextScanner;
import cs437.bsu.search.engine.corpus.Token;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.Text;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.function.BiFunction;

public class Doc {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Doc.class);

    private int id;
    private String title;
    private String path;
    private int highestTokenFreq;

    protected Doc(int id, String title, String path, int highestTokenFreq){
        this.id = id;
        this.title = title;
        this.path = path;
        this.highestTokenFreq = highestTokenFreq;
    }

    public String getTitle(){
        return title;
    }

    public File getDocFile(){
        return new File(path);
    }

    public int getId() {
        return id;
    }

    public int getHighestTokenFreq() {
        return highestTokenFreq;
    }

    public void getDocSnippets(List<String> docs, int position, List<Term> tokens){
        TextScanner ts = TextScanner.getInstance();
        CoreDocument document = ts.scan(loadDocFile());
        TreeSet<Triple<CoreSentence, Integer, Double>> rankedSentences = new TreeSet<>(new Comparator<Triple<CoreSentence, Integer, Double>>() {

            @Override
            public int compare(Triple<CoreSentence, Integer, Double> o1, Triple<CoreSentence, Integer, Double> o2) {
                double diff = o1.c - o2.c;
                if(diff == 0)
                    return o2.b - o1.b;
                else
                    return diff < 0 ? -1 : 1;
            }
        });

        Map<String, Integer> tokenToSentences = new HashMap<>();
        List<Pair<Integer, Map<String, Token>>> sentences = new ArrayList<>();

        LOGGER.debug("Ranking {} sentences.", document.sentences().size());
        for(CoreSentence cs : document.sentences()){
            Map<String, Token> sentenceTokens = ts.getSentenceTokens(cs, ts::removeStopwords, ts::removeNonDictionaryTerms, ts::removeIllegalPatterns, ts::removeLongShortTokens);
            int maxFreqToken = 0;
            for(Map.Entry<String, Token> entry : sentenceTokens.entrySet()){
                if(entry.getValue().getFrequency() > maxFreqToken)
                    maxFreqToken = entry.getValue().getFrequency();

                Integer numSentences = tokenToSentences.get(entry.getKey());
                if(numSentences == null)
                    tokenToSentences.put(entry.getKey(), 1);
                else
                    tokenToSentences.put(entry.getKey(), numSentences + 1);
            }
            sentences.add(new Pair<>(maxFreqToken, sentenceTokens));
        }

        BiFunction<Token, Integer, Double> tf_idf = (Token t, Integer sentencePos) -> {
            double tf = t.getFrequency() / (double) sentences.get(sentencePos).a;
            double idf = sentences.size() / (double) tokenToSentences.get(t.getToken());
            return tf * idf;
        };

        for(int i = 0; i < sentences.size(); i++){
            Pair<Integer, Map<String, Token>> sentence = sentences.get(i);
            double numerator = 0;
            double denominatorQuery = 0;
            double denominatorSentence = 0;

            for(Term t : tokens){
                denominatorQuery += 1;

                if(sentence.b.containsKey(t.getToken())){
                    double token_tfidf = tf_idf.apply(sentence.b.remove(t.getToken()), i);
                    numerator += token_tfidf;
                    denominatorSentence += Math.pow(token_tfidf, 2);
                }
            }

            if(numerator != 0){
                for(Map.Entry<String, Token> entry : sentence.b.entrySet())
                    denominatorSentence += Math.pow(tf_idf.apply(entry.getValue(), i), 2);

                double rank = numerator / (Math.sqrt(denominatorQuery) * Math.sqrt(denominatorSentence));
                LOGGER.debug("Sentence {} was given a rank of: {}", i, rank);
                rankedSentences.add(new Triple<>(document.sentences().get(i), i, rank));
            }else{
                LOGGER.debug("Sentence {} was given a rank of: {}", i, 0);
                rankedSentences.add(new Triple<>(document.sentences().get(i), i, 0d));
            }
        }


        Triple<CoreSentence, Integer, Double> sentence1 = rankedSentences.isEmpty() ? null : rankedSentences.pollLast();
        Triple<CoreSentence, Integer, Double> sentence2 = rankedSentences.isEmpty() ? null : rankedSentences.pollLast();
        LOGGER.debug("Using the top 2 sentences. Sent1={},Sent2={}", sentence1 != null ? sentence1.b : "N/A", sentence2 != null ? sentence2.b : "N/A");

        String snippet;
        if(sentence1 == null) {
            snippet = "This Document has no content.";
        }else{
            snippet = sentence1.a.toString();
            if(sentence2 != null) {
                if (sentence1.b <= sentence2.b)
                    snippet += " " + sentence2.a.toString();
                else
                    snippet = sentence2.a.toString() + " " + snippet;
            }
        }

        snippet = Text.wordWrap(snippet, 100, "   \t");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d) %s%n", position + 1, getTitle()));
        sb.append(snippet);
        sb.append(String.format("   \tLOCATION: %s%n%n", getDocFile().getAbsolutePath()));
        docs.set(position, sb.toString());
    }

    private StringBuilder loadDocFile(){
        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new FileReader(getDocFile()))){
            int lineNum = 0;
            String line;
            while((line = br.readLine()) != null){
                lineNum++;
                if(lineNum >= 2)
                    sb.append(line + System.lineSeparator());
            }
        }catch (Exception e ){
            LOGGER.atError().setCause(e).log("Failed to load doc snippets.");
        }
        return sb;
    }
}
