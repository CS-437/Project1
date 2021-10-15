package cs437.bsu.search.engine.index;

import java.io.File;

public class Doc {

    private String title;
    private String path;
    private int highestTokenFreq;

    protected Doc(String title, String path, int highestTokenFreq){
        this.title = title;
        this.path = path;
        this.highestTokenFreq = highestTokenFreq;
    }

    public String getTitle(){
        return title;
    }

    public File getDocFile(){
        return new File(path);
    }
}
