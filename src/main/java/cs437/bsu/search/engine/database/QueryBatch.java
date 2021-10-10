package cs437.bsu.search.engine.database;

import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class QueryBatch {

    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(QueryBatch.class);

    private PreparedStatement ps;
    private QueryType type;
    private int numParams;
    private int batchSize;

    protected QueryBatch(QueryType type, PreparedStatement ps) throws SQLException {
        this.ps = ps;
        this.type = type;
        this.numParams = ps.getParameterMetaData().getParameterCount();
        batchSize = 0;
    }

    public void set(int index, Object obj){
        LOGGER.debug("Setting query variable.");

        try {
            if(index <= 0 || index > numParams || obj == null) {
                LOGGER.atError().addKeyValue("Index", index).addKeyValue("Param", obj).log("Invalid Index or null object provided.");
                return;
            }

            if (obj instanceof Integer) {
                LOGGER.trace("Setting query index {} for type int.", index);
                ps.setInt(index, (int) obj);
            }else if (obj instanceof Long) {
                LOGGER.trace("Setting query index {} for type long.", index);
                ps.setLong(index, (long) obj);
            }else if (obj instanceof String) {
                LOGGER.trace("Setting query index {} for type String.", index);
                ps.setString(index, (String) obj);
            }else {
                LOGGER.atError().addKeyValue("Param", obj).log("Failed to find a valid type query parameter type.");
            }
        }catch (SQLException e){
            LOGGER.error("Failed to set query parameter.", e);
        }
    }

    public int getBatchSize(){
        return batchSize;
    }

    public void addBatch(){
        LOGGER.info("Add a SQL batch for: {}", type.name());
        try {
            ps.addBatch();
            batchSize++;
        }catch (SQLException e){
            LOGGER.atError().setCause(e).log("Failed to add a Batch for: {}", type.name());
        }
        LOGGER.info("Batch added for: {}", type.name());
    }

    public void executeBatch(){
        LOGGER.info("Executing SQL batch for: {}. Batch Size: {}", type.name(), batchSize);
        try {
            ps.executeBatch();
            batchSize = 0;
        }catch (SQLException e){
            LOGGER.atError().setCause(e).log("Failed to execute Batch for: {}", type.name());
        }
        LOGGER.info("Batch executed for: {}", type.name());
    }

    protected PreparedStatement getStatement(){
        return ps;
    }

    protected QueryType getType(){
        return type;
    }

    protected QueryBatch reset(){
        LOGGER.debug("Resetting query parameters.");
        try {
            ps.clearParameters();
        }catch (SQLException e){
            LOGGER.warn("Failed to rest query parameters.", e);
        }
        return this;
    }

    @Override
    public String toString() {
        return type.name();
    }
}
