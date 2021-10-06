package cs437.bsu.search.engine.corpus;

import cs437.bsu.search.engine.database.Database;
import cs437.bsu.search.engine.database.Query;
import cs437.bsu.search.engine.database.QueryType;
import cs437.bsu.search.engine.util.TaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Document implements DataSaver {

    private Map<String, Token> tokens;
    private String path;
    private String title;
    private int id;

    public Document(String path){
        this.path = path;
        tokens = new HashMap<>();
    }

    public void parse(){
        //TODO: Write how to parse a document
    }

    @Override
    public void saveData() {
        Database db = Database.getInstance();

        Query q = db.getQuery(QueryType.AddDocument);
        q.set(1, id);
        q.set(2, title);
        q.set(3, path);
        db.executeQuery(q);

        AtomicInteger finished = new AtomicInteger();
        for(Token t : tokens.values()) {
            TaskExecutor.StartTask(t::saveData, () -> {
                synchronized (this) {
                    finished.incrementAndGet();
                }
            });
        }

        while (finished.get() != tokens.size());
    }
}
