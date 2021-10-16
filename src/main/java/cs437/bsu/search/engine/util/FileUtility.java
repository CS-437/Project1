package cs437.bsu.search.engine.util;

import java.io.File;
import java.nio.file.Path;

/**
 * Utility class to preform various tasks dealing with files.
 * @author Cade Peterson
 */
public class FileUtility {

    private static Path JAR_LOCATION = new File(System.getProperty("user.dir")).toPath();

    /**
     * Relativizes a file path from the current location
     * to the location the file provided is pointing to.
     * @param f File to get relative path to.
     * @return Relative path from current location in system to the file.
     */
    public static String getRelativeLocation(File f){
        Path filePath = new File(f.getAbsolutePath()).toPath();
        return JAR_LOCATION.relativize(filePath).toString();
    }
}
