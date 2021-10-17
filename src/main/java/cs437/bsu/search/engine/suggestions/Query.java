package cs437.bsu.search.engine.suggestions;

import java.util.HashSet;
import java.util.Set;

public class Query implements Comparable<Query> {

    Set<String> terms;
    String query;
    int position;
    Session session;

    public Query(String query, int position, Session session){
        this.position = position;
        this.session = session;
        this.query = query;
        this.terms = new HashSet<>();

        String[] parts = query.split("\\s+");
        for(int i = 0; i < parts.length; i++)
            this.terms.add(parts[i]);
    }

    public int getID() {

        return this.session.getId();
    }

    public String getQuery(String query) {

        return this.query;
    }


    // Would want to thread this
    // Code adjustments will need to be made
    public Set<Query> getQC(String[] query){
        Set<Query> qcs = new HashSet<>();
        for(int i = position + 1; i < session.getQueries().size(); i++){
            Query possibleQC = (Query) session.getQueries().get(i);
            boolean possible = true;
            for(int j = 0; j < query.length && possible; j++)
                possible = possibleQC.terms.contains(query[j]);

            if(possible)
                qcs.add(possibleQC);
        }
        return qcs;
    }

    @Override
    public int compareTo(Query o) {
        int diff = session.id - o.session.id;
        if(diff == 0)
            diff = query.compareTo(o.query);
        return diff;
    }
}
