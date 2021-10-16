package cs437.bsu.search.engine.util;

/**
 * Utility Class dealing with Text.
 * @author Cade Peterson
 */
public class Text {

    /**
     * Wraps a String after x amount of Characters. Spacing is of the string
     * is removed and replaced with single spaces. This helps with the wrapping.
     * Each line might overshoot the line length to keep words contiguous and
     * un-separated. Note each line of the string is indented.
     * @param s String to wrap.
     * @param lineLength Length of each line.
     * @param indent Indent at each line. Not taken into account for line length.
     * @return Wrapped String.
     */
    public static String wordWrap(String s, int lineLength, String indent){
        StringBuilder sb = new StringBuilder(indent);
        String[] parts = s.split("\\s+");
        int currLength = 0;
        for(String word : parts){
            if(currLength >= lineLength){
                currLength = 0;
                sb.append(System.lineSeparator() + indent);
            }

            sb.append(word + " ");
            currLength += word.length() + 1;
        }
        return sb.toString() + System.lineSeparator();
    }

    /**
     * Dictates if a Character is numerical.
     * @param c Character to check.
     * @return True if between 0-9, otherwise false.
     */
    public static boolean isNumeric(char c){
        return '0' <= c && c <= '9';
    }
}
