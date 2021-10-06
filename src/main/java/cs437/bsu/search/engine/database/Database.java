package cs437.bsu.search.engine.database;

import cs437.bsu.search.engine.util.Properties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Database {

    private static Database DB;

    public static Database getInstance(){
        if(DB == null)
            DB = new Database();
        return DB;
    }

    private Connection connection;
    private Map<QueryType, Query> queries;

    private Database(){

        String address = Properties.DB_Address.loadProperty();
        int port = Properties.DB_Port.loadIntProperty();
        String username = Properties.DB_Username.loadProperty();
        String password = Properties.DB_Password.loadProperty();

        String url = String.format("jdbc:mysql://%s:%d/?verifyServerCertificate=false&useSSL=true&user=%s&password=%s&serverTimezone=UTC", address, port, username, password);

        try{
            connection = DriverManager.getConnection(url);
            Runtime.getRuntime().addShutdownHook(new Thread(new DatabaseShutdownHook()));

            connection.prepareCall("use TokenIndex;").execute();
            setupQueries();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupQueries() throws SQLException {
        queries = new HashMap<>();
        for(Map.Entry<QueryType, String> entry : QueryType.loadQueries().entrySet())
            queries.put(entry.getKey(), new Query(entry.getKey(), connection.prepareStatement(entry.getValue())));
    }

    public Query getQuery(QueryType type){
        return queries.get(type).reset();
    }

    public ResultSet executeQuery(Query q){
        try {
            if (q.getType().isUpdateQuery())
                q.getStatement().executeUpdate();
            else
                return q.getStatement().executeQuery();
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    private static class DatabaseShutdownHook implements Runnable {

        @Override
        public void run() {
            if(DB != null){
                try{
                    for(Map.Entry<QueryType, Query> entry : DB.queries.entrySet())
                        entry.getValue().getStatement().close();
                    DB.connection.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
