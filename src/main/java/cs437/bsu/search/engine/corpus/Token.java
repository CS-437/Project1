package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.database.QueryBatch;
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

    public void saveData(int docId, QueryBatch qb) {
        LOGGER.debug("Starting upload of Token to database. DocId={},Token={}", docId, token);

        long hashvalue = computeHash(token);

        LOGGER.debug("Uploading data: DocId={},token={},hashValue={},frequency={}", docId, token, hashvalue, frequency);
        qb.set(1, docId);
        qb.set(2, token);
        qb.set(3, hashvalue);
        qb.set(4, frequency);
        qb.addBatch();
    }

    private static long computeHash(String token) {
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
}
