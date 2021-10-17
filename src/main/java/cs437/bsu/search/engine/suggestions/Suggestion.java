package cs437.bsu.search.engine.suggestions;

public class Suggestion implements Comparable<Suggestion> {

    String key;
    int freq;

    public Suggestion(String key, int freq) {

        this.freq = freq;
        this.key = key;
    }

    public String getKey() {

        return this.key;
    }

    public int getFreq() {

        return this.freq;
    }

    @Override
    public int compareTo(Suggestion o) {
        if (this.freq > o.getFreq()) {

            return -1;
        }

        else if(this.freq == o.getFreq()) {

            if (this.key.length() < o.getKey().length()) {

                return -1;
            }

            else {

                return 1;
            }
        }

        else {

            return 1;
        }
    }
}
