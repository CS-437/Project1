package cs437.bsu.search.engine.index;

import java.util.HashMap;
import java.util.Map;

public class Term implements Comparable<Term> {

    private String token;
    private long hashValue;
    private Map<Integer, Integer> docFrequencies;

    protected Term(String token, long hashValue){
        this.token = token;
        this.hashValue = hashValue;
        docFrequencies = new HashMap<>();
    }

    public void addDocumentLink(int docId, int freq){
        docFrequencies.put(docId, freq);
    }

    public String getToken() {
        return token;
    }

    public long getHashValue() {
        return hashValue;
    }

    @Override
    public int compareTo(Term o) {
        return token.compareTo(o.token);
    }
}
