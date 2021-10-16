package cs437.bsu.search.engine.index;

import cs437.bsu.search.engine.container.Triple;
import cs437.bsu.search.engine.corpus.Document;
import cs437.bsu.search.engine.corpus.Token;
import cs437.bsu.search.engine.util.FileUtility;
import cs437.bsu.search.engine.util.LoggerInitializer;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to create a Reverse Index from {@link Document Documents}
 * and {@link Token Tokens}. This class is a Singleton.
 * @author Cade Peterson
 */
public class IndexCreator {

    /** Helps Keep track of the current Token ID */
    private static long CURR_TOKEN_PK = 1;

    /** Used for the Database. Amount of entries per Replace command. */
    private static long REPLACE_MAX_ROWS = 10_000;

    /** Max size for a File */
    private static long MAX_DML_SIZE = 900 * (long) Math.pow(1024, 2);

    /** Index File Extensions */
    private static String DML_EXTENSION = ".sql";

    private static IndexCreator INSTANCE;
    private static Logger LOGGER = LoggerInitializer.getInstance().getSimpleLogger(IndexCreator.class);

    /**
     * Gets the instance of this Class.
     * @return Class Instance.
     */
    public static IndexCreator getInstance() {
        if (INSTANCE == null)
            INSTANCE = new IndexCreator();
        return INSTANCE;
    }

    /** Types of Index files and their formats. */
    public enum DMLType {
        /** Document Info File */
        Document(
                "dml_documents",
                "Replace into Documents (DocumentID,HighestTermFreq,Title,Path) VALUES ",
                "%n(%d,%d,\"%s\",\"%s\")"),
        /** Token Info File */
        Token(
                "dml_tokens",
                "Replace into Tokens (TokenPK,Token,HashValue) VALUES ",
                "%n(%d,\"%s\",%d)"),
        /** Intersection Info File */
        Intersection(
                "dml_intersection",
                "Replace into Intersection (TokenFK,DocumentID,Frequency) VALUES ",
                "%n(%d,%d,%d)");

        private String fileName;
        private String replaceCommand;
        private String dmlRowFmt;

        /**
         * Creates the info needed for each file type.
         * @param fileName Base name of the file.
         * @param replaceCommand The SQL command for a database to use.
         * @param dmlRowFmt Format for storing Object Info per line.
         */
        private DMLType(String fileName, String replaceCommand, String dmlRowFmt) {
            this.fileName = fileName;
            this.replaceCommand = replaceCommand;
            this.dmlRowFmt = dmlRowFmt;
        }
    }

    private Map<Long, Map<String, Long>> tokens;
    private Map<DMLType, Triple<File, BufferedWriter, Integer>> dmlWriterMap;

    /**
     * Creates an Index Creator. This class adds a shutdown
     * hook to correctly close and flush the files before this
     * application closes.
     */
    private IndexCreator() {
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

    /**
     * Saves a Document to a Document File.
     * @param highestFreq Term count that has the highest frequency in the document.
     * @param doc Document to save.
     * @param lastDoc True if this is the last document to save.
     */
    public void saveDocumentData(int highestFreq, Document doc, boolean lastDoc) {
        saveData(DMLType.Document, lastDoc, doc.getId(), highestFreq, doc.getTitle(), FileUtility.getRelativeLocation(doc.getFile()).replace("\\", "\\\\"));
    }

    /**
     * Saves a Token to a Token and Intersection File.
     * @param docId ID of the document this token is related to.
     * @param token Token to save.
     * @param lastToken True if this is the last token in the last document being saved.
     */
    public synchronized void saveTokenData(int docId, Token token, boolean lastToken) {
        long tokenPk = -1;
        boolean newToken;

        // Figure out if this token has been saved before
        String tkn = token.getToken();
        long tokenHash = token.getHash();
        Map<String, Long> hashValueMap = tokens.get(tokenHash);
        if (newToken = (hashValueMap == null)) {
            hashValueMap = new HashMap<>();
            tokenPk = CURR_TOKEN_PK++;
            hashValueMap.put(tkn, tokenPk);
            tokens.put(tokenHash, hashValueMap);
        }else{
            Long pk = hashValueMap.get(tkn);
            if (newToken = (pk == null)) {
                tokenPk = CURR_TOKEN_PK++;
                hashValueMap.put(tkn, tokenPk);
            } else {
                tokenPk = pk;
            }
        }

        // Save to a token file if it's a new token.
        if (newToken)
            saveData(DMLType.Token, lastToken, tokenPk, tkn, tokenHash);

        saveData(DMLType.Intersection, lastToken, tokenPk, docId, token.getFrequency());
    }

    /**
     * Saves Data to a file.
     * @param type File type to save to.
     * @param lastItem True if this is the very last item to save of the said type.
     * @param data Data to be saved to the file matching the {@link DMLType#dmlRowFmt type.dmlRowFmt};
     */
    private void saveData(DMLType type, boolean lastItem, Object... data) {
        // Get the current file to be written to for the specified type
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

        // Update file information for future additions
        if (!lastItem)
            updateFile(type, fileData);
    }

    /**
     * Updates the current file. This makes sure replace commands are placed after
     * {@link #REPLACE_MAX_ROWS} has been reached for this file and resets. Also,
     * if this file gets to big another one is created and the old one flushed and
     * closed.
     * @param type Type of file information to update.
     * @param fileData File data to update.
     */
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
