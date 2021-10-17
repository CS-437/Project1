package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.index.IndexCreator;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

/**
 * Represents a Token during pre-processing. Holds information
 * such as Hash-Value for quick lookup and frequency.
 * @author Cade Peterson
 */
public class Token {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Token.class);

    private String token;
    private int frequency;
    private long hash;

    /**
     * Creates a Token. Note that the hash-value is
     * pre-computed here and doesn't need to be done manually.
     * @param token String form of the token.
     */
    protected Token(String token) {
        this.token = token;
        this.frequency = 1;
        hash = getHashValue(token);
    }

    /** Increments the frequency of the token found in the current setting. */
    protected void incrementFrequency(){
        frequency++;
    }

    /**
     * Saves the tokens' data to the {@link IndexCreator}
     * @param docId Document ID the token is associated to.
     * @param lastToken True if this is the last token found
     *                 in the last document, otherwise false.
     */
    public void saveData(int docId, boolean lastToken) {
        LOGGER.debug("Adding Token to DML. DocID={},Token={}", docId, token);
        IndexCreator.getInstance().saveTokenData(docId, this, lastToken);
    }

    /**
     * Computes a hash-value for a string. This helps with speedy lookup.
     * @param s String to compute hash-value for.
     * @return hash in the range of 0 - 2,147,483,647
     */
    public static long getHashValue(String s) {
        long h = 1125899906842597L; // prime
        int len = s.length();

        // Compute Hash
        for (int i = 0; i < len; i++)
            h = 31 * h + s.charAt(i);

        // Make it positive
        if (h < -1)
            h *= -1;

        // Create bound
        h %= 2147483647;
        return h;
    }

    /**
     * Gets the token String value.
     * @return Token in string form.
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets the frequency of the token.
     * @return Frequency of the token 0-N.
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * Gets the hash-value of the token.
     * @return hash-value.
     */
    public long getHash() {
        return hash;
    }
}
