package cs437.bsu.search.engine.index;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Term during the Search Engine Phase.
 * Terms are comparable against one another using the
 * string version of the term.
 * @author Cade Peterson
 */
public class Term implements Comparable<Term> {

    private String token;
    private long hashValue;
    private Map<Integer, Integer> docFrequencies;

    /**
     * Creates a Term.
     * @param token String value of the Term.
     * @param hashValue Hash-Value of the Term.
     */
    public Term(String token, long hashValue){
        this.token = token;
        this.hashValue = hashValue;
        docFrequencies = new HashMap<>();
    }

    /**
     * Adds a Link to the documents in which this term is found.
     * @param docId Document found in.
     * @param freq Frequency this token is found within document.
     */
    public void addDocumentLink(int docId, int freq){
        docFrequencies.put(docId, freq);
    }

    /**
     * Gets the Token String Value.
     * @return String Value.
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets this Token's Hash-Value.
     * @return Hash-Value.
     */
    public long getHashValue() {
        return hashValue;
    }

    /**
     * Gets a Set of Documents IDs
     * this Term is associated to.
     * @return Set of Document IDs this term is related to.
     */
    public Set<Integer> getDocs(){
        return docFrequencies.keySet();
    }

    /**
     * Gets the number of associated documents
     * this term is related to.
     * @return Number of documents this term is found in.
     */
    public int numberAssociatedDocs(){
        return docFrequencies.size();
    }

    /**
     * Given a document a frequency value is provided dictating
     * the number of times this term is found within the document.
     * @param doc Document to get term frequency info for.
     * @return Term frequency in document. 0-n
     */
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
