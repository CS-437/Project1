package cs437.bsu.search.engine.query;

import cs437.bsu.search.engine.corpus.TextScanner;
import cs437.bsu.search.engine.corpus.Token;
import cs437.bsu.search.engine.index.Term;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.TaskExecutor;
import edu.stanford.nlp.pipeline.CoreDocument;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

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
        System.out.println("Your Query was: " + query);
        Collection<Term> tokens = getQueryTokens(query);
        TaskExecutor.sleep(750);
    }

    private Collection<Term> getQueryTokens(String query){
        TextScanner ts = TextScanner.getInstance();
        CoreDocument doc = ts.scan(query);
        Collection<Token> tokens = ts.getDocTokens(doc, ts::removeStopwords, ts::removeNonDictionaryTerms, ts::removeIllegalPatterns, ts::removeLongShortTokens).values();

        Collection<Term> terms = new HashSet<>();
        Iterator<Token> it = tokens.iterator();
        while(it.hasNext()){
            Token t = it.next();
            it.remove();

            
        }
    }
}
