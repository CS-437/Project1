package cs437.bsu.search.engine.util;

public class Text {

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
}
