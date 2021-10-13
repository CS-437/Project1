package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.database.DMLCreator;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

public class Token {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Token.class);

    private String token;
    private int frequency;
    private long hash;

    protected Token(String token) {
        this.token = token;
        this.frequency = 1;
        hash = getHashValue(token);
    }

    protected void incrementFrequency(){
        frequency++;
    }

    public void saveData(int docId, boolean lastToken) {
        LOGGER.debug("Adding Token to DML. DocID={},Token={}", docId, token);
        DMLCreator.getInstance().saveTokenData(docId, this, lastToken);
    }

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

    public String getToken() {
        return token;
    }

    public int getFrequency() {
        return frequency;
    }

    public long getHash() {
        return hash;
    }
}
