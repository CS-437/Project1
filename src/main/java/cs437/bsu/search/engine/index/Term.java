package cs437.bsu.search.engine.index;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class Term implements Comparable<Term> {

    private String token;
    private long hashValue;
    private Map<Integer, Integer> docFrequencies;

    public Term(String token, long hashValue){
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

    public Set<Integer> getDocs(){
        return docFrequencies.keySet();
    }

    public int numberAssociatedDocs(){
        return docFrequencies.size();
    }

    public int getDocFrequency(Doc doc){
        Integer freq = docFrequencies.get(doc.getId());
        if(freq == null)
            return 0;
        return freq;
    }

    @Override
    public int compareTo(Term o) {
        return token.compareTo(o.token);
    }
}
