package cs437.bsu.search.engine.suggestions;

import java.io.*;
import java.util.*;

public class AOLMap {

    private Map<String, Set<Query>> queryLogMap;
    private File aolDir;


    public AOLMap(File aolDir) {

        this.aolDir = aolDir;
        this.queryLogMap = new HashMap<String, Set<Query>>();
    }

    public HashMap<String, Set<Query>> getMap() {

        return (HashMap<String, Set<Query>>) queryLogMap;
    }

    public void run() throws IOException {

        File[] files = aolDir.listFiles();

        assert files != null;

        queryLogMap = new HashMap<String, Set<Query>>();

        for (File f : files) {

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

            br.close();
        }
    }
}
