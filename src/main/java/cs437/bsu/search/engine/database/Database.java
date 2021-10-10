package cs437.bsu.search.engine.database;

import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.Properties;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Database {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Database.class);
    private static Database DB;

    public static Database getInstance(){
        if(DB == null)
            DB = new Database();
        return DB;
    }

    private Connection connection;
    private Map<QueryType, QueryBatch> queries;

    private Database(){
        LOGGER.info("Setting up Database Connection ...");

        String address = Properties.DB_Address.loadProperty();
        int port = Properties.DB_Port.loadIntProperty();
        String username = Properties.DB_Username.loadProperty();
        String password = Properties.DB_Password.loadProperty();

        LOGGER.trace("Connection Target: {}:{}", address, port);
        String url = String.format("jdbc:mysql://%s:%d/?verifyServerCertificate=false&useSSL=true&user=%s&password=%s&serverTimezone=UTC", address, port, username, password);

        try{
            connection = DriverManager.getConnection(url);
            Runtime.getRuntime().addShutdownHook(new Thread(new DatabaseShutdownHook()));

            connection.prepareCall("use TokenIndex;").execute();
            setupQueries();

            LOGGER.info("Database Connection setup.");
        } catch (SQLException e) {
            LOGGER.error("Failed to setup Database Connection.", e);
            System.exit(-1);
        }
    }

    private void setupQueries() throws SQLException {
        LOGGER.debug("Loading Database queries ...");
        queries = new HashMap<>();
        for(Map.Entry<QueryType, String> entry : QueryType.loadQueries().entrySet()) {
            LOGGER.trace("\tAdding Query: {}", entry.getKey().name());
            queries.put(entry.getKey(), new QueryBatch(entry.getKey(), connection.prepareStatement(entry.getValue())));
        }
    }

    public QueryBatch getQuery(QueryType type){
        LOGGER.trace("Query Requested: {}", type);
        return queries.get(type).reset();
    }

//    public ResultSet executeQuery(QueryBatch q){
//        LOGGER.debug("Sending Query: {}", q);
//        try {
//            if (q.getType().isUpdateQuery())
//                q.getStatement().executeUpdate();
//            else
//                return q.getStatement().executeQuery();
//        }catch (SQLException e){
//            LOGGER.error("Failed to send query.", e);
//        }
//        return null;
//    }

    private static class DatabaseShutdownHook implements Runnable {

        @Override
        public void run() {
            LOGGER.info("Closing database connection.");
            if(DB != null){
                try{
                    for(Map.Entry<QueryType, QueryBatch> entry : DB.queries.entrySet())
                        entry.getValue().getStatement().close();
                    DB.connection.close();

                } catch (SQLException e) {
                    LOGGER.info("Failed tp close the database connection.", e);
                }
            }
        }
    }
}
