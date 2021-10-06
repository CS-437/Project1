package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.util.TaskExecutor;

public class Token implements DataSaver {

    private String token;
    private long hashValue;
    private int vocabSize;
    private int collectionSize;
    private int frequency;

    protected Token(String token, int vocabSize, int collectionSize) {
        this.token = token;
        this.vocabSize = vocabSize;
        this.collectionSize = collectionSize;
        this.frequency = 1;

        // Save time in creating this object
        TaskExecutor.StartTask(token, Token::computeHash, (Long hash) -> {this.hashValue = hash;});
    }

    protected void increment(){
        frequency++;
    }

    @Override
    public void saveData() {

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
