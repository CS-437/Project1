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
import org.slf4j.Logger;

import java.util.*;

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
            clearScreen();
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
        List<Pair<String, Term>> tokens = getQueryTokens(query);
        Set<Integer> relevantDocIds = getRelevantDocIds(tokens);
        Set<Doc> docs = getDocuments(relevantDocIds);
        TaskExecutor.sleep(750);
    }

    private List<Pair<String, Term>> getQueryTokens(String query){
        LOGGER.debug("Getting tokens from query.");
        TextScanner ts = TextScanner.getInstance();
        CoreDocument doc = ts.scan(query);
        Collection<Token> tokens = ts.getDocTokens(doc, ts::removeStopwords, ts::removeNonDictionaryTerms, ts::removeIllegalPatterns, ts::removeLongShortTokens).values();

        IndexLoader il = IndexLoader.getInstance();
        List<Pair<String, Term>> terms = new ArrayList<>();

        Iterator<Token> it = tokens.iterator();
        while(it.hasNext()){
            Token t = it.next();
            it.remove();

            Term term = il.getTermByHashToken(t.getHash(), t.getToken());
            LOGGER.trace("Term in query: {}. Found in Index: {}", t.getToken(), term != null);
            terms.add(new Pair<>(t.getToken(), term));
        }
        return terms;
    }

    private Set<Integer> getRelevantDocIds(List<Pair<String, Term>> terms){
        LOGGER.debug("Getting relevant document IDs.");

        // No Tokens provided
        if(terms.isEmpty()) {
            LOGGER.trace("No terms where provided.");
            return new HashSet<>();
        }

        Set<Integer> docIds = new HashSet<>();
        for(int i = 0; i < terms.size(); i++){
            Pair<String, Term> term = terms.get(i);
            if(term != null) {
                if (docIds.isEmpty())
                    docIds.addAll(term.b.getDocs());
                else
                    docIds = intersectSets(docIds, term.b.getDocs());
            }
        }

        if(docIds.size() < 50){
            // {x, [x, x, x, x}, x]
            LOGGER.trace("Not enough relevant documents where found. Using left half of List.");
            docIds.addAll(getRelevantDocIds(terms.subList(0, terms.size() - 2))); // 0 - n-1
            LOGGER.trace("Using right half of List.");
            docIds.addAll(getRelevantDocIds(terms.subList(1, terms.size() - 1))); // 1 - n
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
}
