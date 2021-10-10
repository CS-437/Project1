package cs437.bsu.search.engine.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public enum QueryType {
    AddToken        ("AddToken.sql",        true),
    AddDocument     ("AddDocument.sql",     true);

    private String fileName;
    private boolean isUpdate;

    QueryType(String fileName, boolean isUpdate){
        this.fileName = fileName;
        this.isUpdate = isUpdate;
    }

    protected boolean isUpdateQuery(){
        return isUpdate;
    }

    protected static Map<QueryType, String> loadQueries(){
        Map<QueryType, String> queries = new HashMap<>();

        for(QueryType type : QueryType.values())
            queries.put(type, type.loadQuery());

        return queries;
    }

    private String loadQuery() {
        InputStream resource = QueryType.class.getResourceAsStream(fileName);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource))){
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);
        }catch (IOException e){
            e.printStackTrace();
        }
        return sb.toString();
    }
}
