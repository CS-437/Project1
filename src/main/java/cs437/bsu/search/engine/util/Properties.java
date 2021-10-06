package cs437.bsu.search.engine.util;

public enum Properties{
    DB_Username ("database.credential.username"),
    DB_Password ("database.credential.password"),
    DB_Address ("database.connection.address"),
    DB_Port ("database.connection.port");

    private String key;

    Properties(String key){
        this.key = key;
    }

    public String loadProperty(){
        return System.getProperty(key);
    }

    public int loadIntProperty(){
        return Integer.parseInt(System.getProperty(key));
    }
}