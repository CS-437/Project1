package cs437.bsu.search.engine.database;

import cs437.bsu.search.engine.container.Pair;
import cs437.bsu.search.engine.container.Triple;
import cs437.bsu.search.engine.corpus.Document;
import cs437.bsu.search.engine.corpus.Token;
import cs437.bsu.search.engine.util.LoggerInitializer;
import cs437.bsu.search.engine.util.PathRelavizor;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMLCreator {

    private static long CURR_TOKEN_PK = 1;
    private static long REPLACE_MAX_ROWS = 10_000;
    private static long MAX_DML_SIZE = 900 * (long) Math.pow(1024, 2);
    private static String DML_EXTENSION = ".sql";

    private static DMLCreator INSTANCE;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(DMLCreator.class);

    public static DMLCreator getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DMLCreator();
        return INSTANCE;
    }

    public enum DMLType {
        Document(
                "dml_documents",
                "Replace into Documents (DocumentID,HighestTermFreq,Title,Path) VALUES ",
                "%n(%d,%d,\"%s\",\"%s\")"),
        Token(
                "dml_tokens",
                "Replace into Tokens (TokenPK,Token,HashValue) VALUES ",
                "%n(%d,\"%s\",%d)"),

        Intersection(
                "dml_intersection",
                "Replace into Intersection (TokenFK,DocumentID,Frequency) VALUES ",
                "%n(%d,%d,%d)");

        private String fileName;
        private String replaceCommand;
        private String dmlRowFmt;

        private DMLType(String fileName, String replaceCommand, String dmlRowFmt) {
            this.fileName = fileName;
            this.replaceCommand = replaceCommand;
            this.dmlRowFmt = dmlRowFmt;
        }
    }

    private Map<Long, Pair<List<String>, List<Long>>> tokens;
    private Map<DMLType, Triple<File, BufferedWriter, Integer>> dmlWriterMap;

    private DMLCreator() {
        tokens = new HashMap<>();
        dmlWriterMap = new HashMap<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Triple<File, BufferedWriter, Integer> file : dmlWriterMap.values()) {
                try {
                    if (file != null) {
                        file.b.close();
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to close DML Writer.", e);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }));

        try {
            for (DMLType type : DMLType.values()) {
                File f = new File(type.fileName + "-1" + DML_EXTENSION);
                dmlWriterMap.put(type, new Triple<>(f, new BufferedWriter(new FileWriter(f)), 1));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to setup writer for one or more DML(s).", e);
            System.exit(-1);
        }
    }

    public void saveDocumentData(int highestFreq, Document doc, boolean lastDoc) {
        saveData(DMLType.Document, lastDoc, doc.getId(), highestFreq, doc.getTitle(), PathRelavizor.getRelativeLocation(doc.getFile()).replace("\\", "\\\\"));
    }

    public void saveTokenData(int docId, Token token, boolean lastToken) {
        long tokenPk = -1;
        boolean newToken;

        String tkn = token.getToken();
        long tokenHash = token.getHash();
        Pair<List<String>, List<Long>> pair = tokens.get(tokenHash);
        if (newToken = (pair == null)) {
            pair = new Pair<>(new ArrayList<>(), new ArrayList<>());
            pair.a.add(tkn);
            tokenPk = CURR_TOKEN_PK++;
            pair.b.add(tokenPk);
            tokens.put(tokenHash, pair);
        }

        if (!newToken) {
            int index = pair.a.indexOf(tkn);
            if (index == -1) {
                pair.a.add(tkn);
                tokenPk = CURR_TOKEN_PK++;
                pair.b.add(tokenPk);
            } else {
                tokenPk = pair.b.get(index);
            }
        }

        if (newToken)
            saveData(DMLType.Token, lastToken, tokenPk, tkn, tokenHash);

        if (tokenPk > 0)
            saveData(DMLType.Intersection, lastToken, tokenPk, docId, token.getFrequency());
    }

    private void saveData(DMLType type, boolean lastItem, Object... data) {
        Triple<File, BufferedWriter, Integer> fileData = dmlWriterMap.get(type);

        try {
            if (fileData.c == 1)
                fileData.b.write(type.replaceCommand);

            fileData.b.write(String.format(type.dmlRowFmt, data));

            if (lastItem)
                fileData.b.write(";");
        } catch (IOException e) {
            LOGGER.atError().setCause(e).log("Failed to add row for type: {}", type);
        }

        if (!lastItem)
            updateFile(type, fileData);
    }

    private void updateFile(DMLType type, Triple<File, BufferedWriter, Integer> fileData) {
        try {
            if (fileData.a.length() >= MAX_DML_SIZE) {
                fileData.b.write(";");
                fileData.b.close();
                String oldFileName = fileData.a.getName();
                int num = Integer.parseInt(oldFileName.substring(oldFileName.indexOf("-") + 1, oldFileName.indexOf("."))) + 1;
                fileData.a = new File(type.fileName + "-" + num + DML_EXTENSION);
                fileData.b = new BufferedWriter(new FileWriter(fileData.a));
                fileData.c = 1;
            } else {
                if (fileData.c == REPLACE_MAX_ROWS) {
                    fileData.b.write(";");
                    fileData.b.newLine();
                    fileData.c = 1;
                } else {
                    fileData.b.write(",");
                    fileData.c++;
                }
            }
        } catch (IOException e) {
            LOGGER.atError().setCause(e).log("Issue occurred with updating file: {}", fileData.a.getAbsolutePath());
        }
    }

}
