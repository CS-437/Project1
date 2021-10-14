package cs437.bsu.search.engine.index;

import java.util.HashMap;
import java.util.Map;

public class Token {

    private String token;
    private long hashValue;
    private Map<Integer, Integer> docFrequencies;

    protected Token(String token, long hashValue){
        this.token = token;
        this.hashValue = hashValue;
        docFrequencies = new HashMap<>();
    }

    public void addDocumentLink(int docId, int freq){
        docFrequencies.put(docId, freq);
    }
}
