package cs437.bsu.search.engine.index;

public class Document {

    private String title;
    private String path;
    private int highestTokenFreq;

    protected Document(String title, String path, int highestTokenFreq){
        this.title = title;
        this.path = path;
        this.highestTokenFreq = highestTokenFreq;
    }

}
