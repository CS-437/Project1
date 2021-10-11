package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.database.DMLCreator;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

public class Token {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Token.class);

    private String token;
    private int frequency;

    protected Token(String token) {
        this.token = token;
        this.frequency = 1;
    }

    protected void incrementFrequency(){
        frequency++;
    }

    public void saveData(int docId, boolean lastToken) {
        LOGGER.debug("Adding Token to DML. DocID={},Token={}", docId, token);
        DMLCreator.getInstance().saveTokenData(docId, this, lastToken);
    }

    public long getHashValue() {
        long h = 1125899906842597L; // prime
        int len = token.length();

        // Compute Hash
        for (int i = 0; i < len; i++)
            h = 31 * h + token.charAt(i);

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
}
