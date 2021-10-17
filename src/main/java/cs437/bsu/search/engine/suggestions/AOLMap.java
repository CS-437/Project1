package cs437.bsu.search.engine.suggestions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class AOLMap {

    private Map<String, Set<Query>> queryLogMap;


    public AOLMap() {

        this.queryLogMap = new HashMap<String, Set<Query>>();
    }

    public void run() throws IOException {

        String prefixPath = "cs437.bsu.search.engine.aol/Clean-Data-0";
        long start = System.currentTimeMillis();

        for (int i = 1; i < 6; i++) {

            String filePath = prefixPath + i + ".txt";
            File f = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(f));

            //Discard first line
            String line = br.readLine();
            line = br.readLine();

            int currentUID = -1;
            int pos = 0;

            Session session = null;
            ArrayList<String> queries = new ArrayList<String>();

            while (line != null) {

                String[] itemAttributes = line.split("\t");
                int uid = Integer.parseInt(itemAttributes[0]);
                String query = itemAttributes[1];

                if (currentUID == -1) {

                    queryLogMap = new HashMap<String, Set<Query>>();
                    queryLogMap.put(query, new HashSet<Query>());
                    session = new Session(uid, new ArrayList<Query>());
                    queries = new ArrayList<String>();

                    currentUID = uid;
                    session.getQueries().add(new Query(query, pos, session));

                    queries.add(query);

                    pos++;
                } else if (uid != currentUID) {

                    pos = 0;
                    session = new Session(uid, new ArrayList<Query>());
                    queries = new ArrayList<String>();

                    currentUID = uid;
                    session.getQueries().add(new Query(query, pos, session));
                    queries.add(query);

                    if (queryLogMap.get(query) == null) {

                        queryLogMap.put(query, new HashSet<Query>());
                    }

                    queryLogMap.get(query).add(new Query(query, pos, session));
                    pos++;

                } else {

                    if (!queries.contains(query)) {

                        session.getQueries().add(new Query(query, pos, session));
                        queries.add(query);

                        if (queryLogMap.get(query) == null) {

                            queryLogMap.put(query, new HashSet<Query>());
                        }

                        queryLogMap.get(query).add(new Query(query, pos, session));
                        pos++;
                    }
                }

                line = br.readLine();
            }
        }
    }
}
