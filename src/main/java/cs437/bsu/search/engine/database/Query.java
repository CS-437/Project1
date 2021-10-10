package cs437.bsu.search.engine.database;

import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Query {

    private static final Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(Query.class);

    private PreparedStatement ps;
    private QueryType type;
    private int numParams;

    protected Query(QueryType type, PreparedStatement ps) throws SQLException {
        this.ps = ps;
        this.type = type;
        this.numParams = ps.getParameterMetaData().getParameterCount();
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

    protected PreparedStatement getStatement(){
        return ps;
    }

    protected QueryType getType(){
        return type;
    }

    protected Query reset(){
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
