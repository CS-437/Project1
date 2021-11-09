package cs437.bsu.search.engine.suggestions;

import java.util.ArrayList;

public class Session {

    int id;
    ArrayList<Query> queries;

    public Session(int id, ArrayList<Query> queries) {

        this.id = id;
        this.queries = queries;
    }

    public int getId() {

        return id;
    }

    public ArrayList<Query> getQueries() {

        return queries;
    }
}
