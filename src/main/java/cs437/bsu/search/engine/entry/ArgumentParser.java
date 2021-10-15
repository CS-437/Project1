package cs437.bsu.search.engine.entry;

import java.io.*;
import java.util.Properties;

public class ArgumentParser {

    private static final String APPLICATION_NAME = "search-engine.jar";
    private static final String INVALID_ARGS =
            String.format("Invalid Argument(s) supplied.%nPlease refer to the help menu:%n\tjava -jar %s -h", APPLICATION_NAME);

    public static ApplicationType application;

    public enum ApplicationType{CreateIndex, SearchEngine}

    private boolean validArgs;
    private String errorRsn;
    private File dir;
    private File aolDir;

    public ArgumentParser(String[] args){
        validArgs = true;
        errorRsn = INVALID_ARGS;

        if(args.length == 0)
            help();

        for(int i = 0; i < args.length && validArgs; i++){
            switch (args[i].toLowerCase()){
                case "-h":
                case "--help":
                    help();
                    break;
                case "-c":
                case "--config-file":
                    i = loadConfigFile(i, args);
                    break;
                case "-t":
                case "--type":
                    i = loadApplication(i, args);
                    break;
                default:
                    validArgs = false;
                    break;
            }
        }

        if(!validArgs){
            System.err.println(errorRsn);
            System.exit(-1);
        }
    }

    public File getDirectory(){
        return dir;
    }
    public File getAolDir() { return aolDir; }

    private int loadApplication(int loc, String[] args){
        loc++;

        if(checkLength(loc, args)){
            errorRsn = "No application type was provided.";
            return -1;
        }

        for(ApplicationType type : ApplicationType.values()){
            if(type.name().toLowerCase().equals(args[loc].toLowerCase())){
                application = type;
                break;
            }
        }

        if(application == null){
            validArgs = false;
            errorRsn = String.format("Invalid application type was provided: %s", args[loc]);
            return -1;
        }

        switch (application){
            case CreateIndex:
                loc++;
                if(checkLength(loc, args)){
                    errorRsn = "No directory to corpus was provided.";
                    return -1;
                }

                dir = new File(args[loc]);
                if(!dir.exists() || !dir.isDirectory()){
                    validArgs = false;
                    errorRsn = String.format("Invalid indexing directory provided: %s", args[loc]);
                }
                break;
            default:
                //INDEX
                loc++;
                if(checkLength(loc, args)){
                    errorRsn = "No directory to index was provided.";
                    return -1;
                }

                dir = new File(args[loc]);
                if(!dir.exists() || !dir.isDirectory()){
                    validArgs = false;
                    errorRsn = String.format("Invalid index directory provided: %s", args[loc]);
                }


                //AOL
                loc++;
                if(checkLength(loc, args)){
                    errorRsn = "No directory to aol query logs was provided.";
                    return -1;
                }

                aolDir = new File(args[loc]);
                if(!aolDir.exists() || !aolDir.isDirectory()){
                    validArgs = false;
                    errorRsn = String.format("Invalid aol query log directory provided: %s", args[loc]);
                }

                break;
        }

        return loc;
    }

    private int loadConfigFile(int loc, String[] args){
        loc++;

        if(checkLength(loc, args)) {
            errorRsn = "No configuration file was provided.";
            return -1;
        }

        File f = new File(args[loc]);
        if (f.exists() && f.isFile()) {
            Properties p = new Properties();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                p.load(br);
            } catch (IOException e) {
                validArgs = false;
                errorRsn = "Failed to load configuration file.";
            }

            for(String prop : p.stringPropertyNames())
                System.setProperty(prop, p.getProperty(prop));

        } else {
            validArgs = false;
            errorRsn = String.format("Invalid configuration file provided - %s", args[loc]);
        }
        return loc;
    }

    private void help(){
        InputStream manPage = ArgumentParser.class.getResourceAsStream("man-page.txt");
        try(BufferedReader br = new BufferedReader(new InputStreamReader(manPage))){
            String line = "";
            while((line = br.readLine()) != null)
                System.out.println(line);
        }catch (IOException e) {
            System.err.printf("Failed to load man page.%nPlease try again.%n");
        }
        System.exit(0);
    }

    private boolean checkLength(int loc, String[] args){
        boolean invalidLength = loc >= args.length;
        validArgs = !invalidLength;
        return invalidLength;
    }
}
