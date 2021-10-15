package cs437.bsu.search.engine.index;

import java.io.File;

public class Doc {

    private int id;
    private String title;
    private String path;
    private int highestTokenFreq;

    protected Doc(int id, String title, String path, int highestTokenFreq){
        this.id = id;
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

    public int getId() {
        return id;
    }

    public int getHighestTokenFreq() {
        return highestTokenFreq;
    }
}
