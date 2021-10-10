package cs437.bsu.search.engine.util;

import java.io.File;
import java.nio.file.Path;

public class PathRelavizor {

    private static Path JAR_LOCATION = new File(System.getProperty("user.dir")).toPath();

    public static String getRelativeLocation(File f){
        Path filePath = new File(f.getAbsolutePath()).toPath();
        return JAR_LOCATION.relativize(filePath).toString();
    }
}
