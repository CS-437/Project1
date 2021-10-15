package cs437.bsu.search.engine.index;

public class Doc {

    private String title;
    private String path;
    private int highestTokenFreq;

    protected Doc(String title, String path, int highestTokenFreq){
        this.title = title;
        this.path = path;
        this.highestTokenFreq = highestTokenFreq;
    }

}
