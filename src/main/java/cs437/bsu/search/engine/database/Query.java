package cs437.bsu.search.engine.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Query {

    private PreparedStatement ps;
    private QueryType type;

    protected Query(QueryType type, PreparedStatement ps){
        this.ps = ps;
        this.type = type;
    }

    public void set(int index, Object obj){
        if(index <= 0)
            return;

        try {
            if (obj.getClass().equals(Integer.class))
                ps.setInt(index, (int) obj);
            if (obj.getClass().equals(Long.class))
                ps.setLong(index, (long) obj);
            else if (obj.getClass().equals(String.class))
                ps.setString(index, (String) obj);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    protected PreparedStatement getStatement(){
        return ps;
    }

    protected QueryType getType(){
        return type;
    }

    protected Query reset(){
        try {
            ps.clearParameters();
        }catch (SQLException e){}
        return this;
    }
}
