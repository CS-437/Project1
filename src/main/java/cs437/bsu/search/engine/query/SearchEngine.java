package cs437.bsu.search.engine.query;

import cs437.bsu.search.engine.container.Pair;
import cs437.bsu.search.engine.corpus.TextScanner;
import cs437.bsu.search.engine.corpus.Token;
import cs437.bsu.search.engine.index.IndexLoader;
import cs437.bsu.search.engine.index.Term;
import cs437.bsu.search.engine.index.Doc;
import cs437.bsu.search.engine.suggestions.AOLMap;
import cs437.bsu.search.engine.suggestions.Query;
import cs437.bsu.search.engine.suggestions.Suggestion;
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

/**
 * Search Engine Application. Facilitates all functions of asking the
 * user for input and providing back query suggestions and hopefully
 * relevant resources.
 * <p>
 * When started the user will then be presented with a prompt on
 * the terminal.
 * @author Cade Peterson
 */
public class SearchEngine extends Thread {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(SearchEngine.class);
    private static final String EXIT_KEYWORD = "exit()";

    private boolean exit;
    private Scanner queryReader;
    private String newScreen;
    private AOLMap aolMap;

    /**
     * Creates the Search Engine. Note the {@link TextScanner} is loaded and might
     * delay this method invocation a bit, however this prevents further lag in
     * future requests for it.
     */
    public SearchEngine(AOLMap aolMap){
        exit = false;
        this.aolMap = aolMap;

        queryReader = new Scanner(System.in);

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 70; i++)
            sb.append(System.lineSeparator());

        newScreen = sb.toString();

        //Load items if instance hasn't been setup yet.
        TextScanner.getInstance();
    }

    @Override
    public void run() {
        clearScreen();

        boolean setSuggestion = false;
        ArrayList<String> prevSugg = null;

        while (!exit){

            String query;
            System.out.print("\nPlease enter a query: ");
            query = queryReader.nextLine();

            LOGGER.info("Query provided: {}", query);

            if(query.equalsIgnoreCase(EXIT_KEYWORD)){
                exit = true;
                continue;
            }

            if(prevSugg != null) {
                for(int i = 0; i < prevSugg.size(); i++) {

                    String test = query;
                    String prev = (i+1) + ".";

                    if(test.equals(prev)) {

                        query = prevSugg.get(i);
                    }
                }
            }

            processQuery(query);
            prevSugg = getSuggestions(query);
        }
        System.out.println("Exiting Search Engine.");
        LOGGER.info("Closing Search Engine.");
    }

    private ArrayList<String> getSuggestions(String query) {

        Map<String, Set<Query>> queryLogMap = aolMap.getMap();
        Set<Query> querySessions = queryLogMap.get(query);
        String[] parts = query.split("\\s+");
        ArrayList<String> ret = new ArrayList<String>(5);

        if (querySessions != null) {

            // # of sessions in which q' is modified to CQ = qcToItsFreq.get(qc);
            Map<String, Integer> qcToItsFreq = new HashMap<>();

            Iterator<Query> it = querySessions.iterator();
            while (it.hasNext()) {
                Query curr = it.next();

                for (Query qc : curr.getQC(parts)) {
                    Integer i = qcToItsFreq.get(qc.getQuery(query));

                    if (i == null)
                        qcToItsFreq.put(qc.getQuery(query), 1);
                    else
                        qcToItsFreq.put(qc.getQuery(query), i + 1);
                }
            }

            PriorityQueue<Suggestion> suggestion = calculate(querySessions, qcToItsFreq);
            System.out.println("---------------------------------------------------------");


            if (suggestion.size() > 0) {

                System.out.println("Instead of \"" + query + "\" would you like to search for: ");
                for (int i = 0; i < 5; i++) {

                    Suggestion sugg = suggestion.poll();

                    if(sugg != null) {

                        System.out.println("        " + (i+1) + ". " + sugg.getKey() + " ---> Enter " + (i+1) + ".");
                        ret.add(sugg.getKey());
                    }
                    else {

                        break;
                    }
                }
            } else {

                System.out.println("No suggestions found for this query\n");
            }
        }

        return ret;
    }

    public static PriorityQueue<Suggestion> calculate(Set<Query> querySet, Map<String, Integer> qcFreq) {

        PriorityQueue<Suggestion> topFive = new PriorityQueue<Suggestion>();

        int lowFreq = 0;
        int highFreq = 0;

        for (String s : qcFreq.keySet()) {

            Suggestion add = new Suggestion(s, qcFreq.get(s));
            topFive.add(add);
        }

        return topFive;
    }

    /**
     * Clears the current Terminal Window.
     */
    private void clearScreen(){
        System.out.println(newScreen);
        LOGGER.trace("Command Line Screen Cleared.");
    }

    /**
     * Processes a Query and gathers relevant documents
     * associated to the query.
     * @param query Query to process.
     */
    private void processQuery(String query){
        LOGGER.info("Processing Query: {}", query);
        List<Term> tokens = getQueryTokens(query);

        // If the process query has no tokens
        // then there is no need to check for documents.
        if(!tokens.isEmpty()) {
            Set<Integer> relevantDocIds = getRelevantDocIds(tokens);
            Set<Doc> docs = getDocuments(relevantDocIds);
            List<Doc> top5Docs = rankDocs(docs, tokens);
            printDocuments(top5Docs, tokens);
        }else{
            System.out.printf("%n\tYour Query '%s' didn't match any of the documents.%n%n%n", query);
        }
    }

    /**
     * Given a Query String it is pre-processed
     * and Terms and a list of terms provided.
     * @param query Query to processes.
     * @return List of pre-processed query terms.
     */
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

    /**
     * Given a list of Query terms a set of Doc ids returned. Note this doc ids are
     * not ranked and range from very to not very relevant.
     * <p>
     * This is a recursive function and will only start to recursively find document
     * ids if less than 50 documents are found for all terms provided. Each recursive
     * call will use n-1 terms for finding relevant documents. It will only stop
     * this recursive dissent once 50+ documents are found or the list only contains
     * one term.
     * @param terms Terms to get associated doc Ids for.
     * @return a Set of associated Doc IDs.
     */
    private Set<Integer> getRelevantDocIds(List<Term> terms){
        LOGGER.debug("Getting relevant document IDs.");

        // No Tokens provided
        if(terms.isEmpty()) {
            LOGGER.trace("No terms where provided.");
            return new HashSet<>();
        }

        // Find all documents that all terms can be found in
        Set<Integer> docIds = new HashSet<>();
        for(Term t : terms){
            if (docIds.isEmpty())
                docIds.addAll(t.getDocs());
            else
                docIds = intersectSets(docIds, t.getDocs());
        }

        // Check if recursion should and can be done.
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

    /**
     * Intersects two sets. Neither set is modified.
     * @param one Set one to intersect with set two.
     * @param two Set two to intersect with set one.
     * @param <T> Type of objects to intersect over.
     * @return New set representing intersection.
     */
    private <T> Set<T> intersectSets(Set<T> one, Set<T> two){
        Set<T> intersection = new HashSet<>();
        for(T t : one){
            if(two.contains(t))
                intersection.add(t);
        }
        return intersection;
    }

    /**
     * Gathers all associated documents to the
     * document IDs provided.
     * @param docIds Document IDs to get documents for.
     * @return Set of Documents found from Document IDs provided.
     */
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

    /**
     * Ranks documents given a set of Documents and Query Terms.
     * @param docs Documents to Rank.
     * @param tokens Query Tokens to help with ranking.
     * @return Top 5 ranked documents. Range 0-5.
     */
    private List<Doc> rankDocs(Set<Doc> docs, List<Term> tokens){
        // Tree set sorting the documents as they are added
        // Sorted in ascending order
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

        // Ranked Documents
        for(Doc doc : docs){
            double rank = rankDocument(doc, tokens);
            top5.add(new Pair<>(doc, rank));
            LOGGER.debug("Ranking Document {}: {}", doc.getTitle(), rank);

            if(top5.size() > 5)
                top5.pollFirst();
        }

        // Get top 5 Ranked Documents
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

    /**
     * Ranks a given Document the query Terms provided.
     * @param doc Document to Rank.
     * @param terms Terms to help with ranking.
     * @return Ranking score for the document.
     */
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

    /**
     * Prints the retrieved set of documents to the terminal.
     * @param docs Documents to print.
     * @param tokens Query Terms to help with generating the document snippets.
     */
    private void printDocuments(List<Doc> docs, List<Term> tokens){
        List<String> docsSnippets = new ArrayList<>();
        List<Boolean> docSnipsDone = new ArrayList<>();

        // Generate Document Snippets
        // Each is threaded for speed
        for(int i = 0; i < docs.size(); i++){
            docsSnippets.add(i, "");
            docSnipsDone.add(i, false);
            final int loc = i;
            TaskExecutor.StartTask(() -> { docs.get(loc).getDocSnippets(docsSnippets, loc, tokens);},
                    () -> {docSnipsDone.set(loc, true);});
        }

        // Loop until all snippets are generated.
        boolean allDone = false;
        while(!allDone){
            boolean currentCheck = true;
            for(boolean b : docSnipsDone)
                currentCheck &= b;

            allDone = currentCheck;
        }

        // Print Snippets
        System.out.println();
        for(String s : docsSnippets)
            System.out.println(s);
        System.out.println();
    }
}
