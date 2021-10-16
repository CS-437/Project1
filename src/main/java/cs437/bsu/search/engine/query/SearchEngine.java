package cs437.bsu.search.engine.query;

import cs437.bsu.search.engine.container.Pair;
import cs437.bsu.search.engine.corpus.TextScanner;
import cs437.bsu.search.engine.corpus.Token;
import cs437.bsu.search.engine.index.IndexLoader;
import cs437.bsu.search.engine.index.Term;
import cs437.bsu.search.engine.index.Doc;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.TaskExecutor;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.util.Index;
import org.apache.lucene.search.Sort;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.Phaser;

public class SearchEngine extends Thread {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(SearchEngine.class);
    private static final String EXIT_KEYWORD = "exit()";

    private boolean exit;
    private Scanner queryReader;
    private String newScreen;

    public SearchEngine(){
        exit = false;
        queryReader = new Scanner(System.in);

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 60; i++)
            sb.append(System.lineSeparator());

        newScreen = sb.toString();

        //Load items if instance hasn't been setup yet.
        TextScanner.getInstance();
    }

    @Override
    public void run() {
        clearScreen();

        while (!exit){
            System.out.print("Please enter a query: ");
            String query = queryReader.nextLine();
            LOGGER.info("Query provided: {}", query);

            if(query.equalsIgnoreCase(EXIT_KEYWORD)){
                exit = true;
                continue;
            }

            processQuery(query);
        }
        System.out.println("Exiting Search Engine.");
        LOGGER.info("Closing Search Engine.");
    }

    private void clearScreen(){
        System.out.println(newScreen);
        LOGGER.trace("Command Line Screen Cleared.");
    }

    private void processQuery(String query){
        LOGGER.info("Processing Query: {}", query);
        List<Term> tokens = getQueryTokens(query);

        if(!tokens.isEmpty()) {
            Set<Integer> relevantDocIds = getRelevantDocIds(tokens);
            Set<Doc> docs = getDocuments(relevantDocIds);
            List<Doc> top5Docs = rankDocs(docs, tokens);
            printDocuments(top5Docs, tokens);
        }else{
            System.out.printf("%n\tYour Query '%s' didn't match any of the documents.%n%n%n", query);
        }
    }

    private List<Term> getQueryTokens(String query){
        LOGGER.debug("Getting tokens from query.");
        TextScanner ts = TextScanner.getInstance();
        CoreDocument doc = ts.scan(query);
        Collection<Token> tokens = ts.getDocTokens(doc, ts::removeStopwords, ts::removeNonDictionaryTerms, ts::removeIllegalPatterns, ts::removeLongShortTokens).values();

        IndexLoader il = IndexLoader.getInstance();
        List<Term> terms = new ArrayList<>();

        Iterator<Token> it = tokens.iterator();
        while(it.hasNext()){
            Token t = it.next();
            it.remove();

            Term term = il.getTermByHashToken(t.getHash(), t.getToken());
            LOGGER.trace("Term in query: {}. Found in Index: {}", t.getToken(), term != null);
            if(term != null)
                terms.add(term);
        }
        return terms;
    }

    private Set<Integer> getRelevantDocIds(List<Term> terms){
        LOGGER.debug("Getting relevant document IDs.");

        // No Tokens provided
        if(terms.isEmpty()) {
            LOGGER.trace("No terms where provided.");
            return new HashSet<>();
        }

        Set<Integer> docIds = new HashSet<>();
        for(Term t : terms){
            if (docIds.isEmpty())
                docIds.addAll(t.getDocs());
            else
                docIds = intersectSets(docIds, t.getDocs());
        }

        if(docIds.size() < 50 && terms.size() >= 2){
            LOGGER.trace("Haven't found enough documents. Search a sub section of the list.");
            for(int i = 0; i < terms.size(); i++) {
                if(i == 0) {
                    docIds.addAll(getRelevantDocIds(terms.subList(1, terms.size()))); // 0 - n-1
                }else if(i == terms.size() - 1) {
                    docIds.addAll(getRelevantDocIds(terms.subList(0, terms.size() - 1))); // 0 - n-1
                }else {
                    List<Term> subList = terms.subList(0, i);
                    subList.addAll(terms.subList(i + 1, terms.size()));
                    docIds.addAll(getRelevantDocIds(subList));
                }
            }
        }

        if(LOGGER.isTraceEnabled()){
            StringBuilder sb = new StringBuilder("Docs Found:");
            for(int docId : docIds)
                sb.append(" " + docId);
            LOGGER.trace(sb.toString());
        }

        return docIds;
    }

    private <T> Set<T> intersectSets(Set<T> one, Set<T> two){
        Set<T> intersection = new HashSet<>();
        for(T t : one){
            if(two.contains(t))
                intersection.add(t);
        }
        return intersection;
    }

    private Set<Doc> getDocuments(Set<Integer> docIds){
        LOGGER.debug("Getting relevant documents.");
        Set<Doc> docs = new HashSet<>();
        IndexLoader il = IndexLoader.getInstance();
        for(int id : docIds) {
            Doc doc = il.getDocById(id);
            docs.add(doc);
            LOGGER.trace("Found document: {}", doc.getTitle());
        }
        return docs;
    }

    private List<Doc> rankDocs(Set<Doc> docs, List<Term> tokens){
        TreeSet<Pair<Doc, Double>> top5 = new TreeSet<>(new Comparator<Pair<Doc, Double>>() {
            @Override
            public int compare(Pair<Doc, Double> o1, Pair<Doc, Double> o2) {
                double diff = o1.b - o2.b;
                if(diff == 0)
                    return 0;
                else
                    return diff < 0 ? -1 : 1;
            }
        });

        for(Doc doc : docs){
            double rank = rankDocument(doc, tokens);
            top5.add(new Pair<>(doc, rank));
            LOGGER.debug("Ranking Document {}: {}", doc.getTitle(), rank);

            if(top5.size() > 5)
                top5.pollFirst();
        }

        StringBuilder sb = new StringBuilder();
        List<Doc> bestTop5 = new ArrayList<>();
        while(!top5.isEmpty()) {
            Doc d = top5.pollLast().a;
            sb.append(" " + d.getTitle());
            bestTop5.add(d);
        }
        LOGGER.debug("Top 5 Documents:{}", sb);

        return bestTop5;
    }

    public static double rankDocument(Doc doc, List<Term> terms){
        double sum = 0;
        double base  = Math.log(2);
        for(Term t : terms) {
            double tf = t.getDocFrequency(doc) / (double) doc.getHighestTokenFreq();
            double idf = Math.log(IndexLoader.getInstance().getNumDocs() / (double) t.numberAssociatedDocs()) / base;
            sum += tf * idf;
        }
        return sum;
    }

    private void printDocuments(List<Doc> docs, List<Term> tokens){
        List<String> docsSnippets = new ArrayList<>();
        List<Boolean> docSnipsDone = new ArrayList<>();

        for(int i = 0; i < docs.size(); i++){
            docsSnippets.add(i, "");
            docSnipsDone.add(i, false);
            final int loc = i;
            TaskExecutor.StartTask(() -> { docs.get(loc).getDocSnippets(docsSnippets, loc, tokens);},
                    () -> {docSnipsDone.set(loc, true);});
        }

        boolean allDone = false;
        while(!allDone){
            boolean currentCheck = true;
            for(boolean b : docSnipsDone)
                currentCheck &= b;

            allDone = currentCheck;
        }

        System.out.println();
        for(String s : docsSnippets)
            System.out.println(s);
        System.out.println();
    }
}
