package querySearchServices;

import java.math.BigDecimal;

public class QuerySearchResObj implements Comparable<QuerySearchResObj> {
    private final String path;
    private final String contents;
    private final BigDecimal score;
    public String getPath() {
        return path;
    }
    public String getContents() {
        return contents;
    }
    public BigDecimal getScore() {
        return score;
    }
    public QuerySearchResObj(
            String path,
            String contents,
            BigDecimal score) {
        this.path = path;
        this.contents = contents;
        this.score = score;
    }
    @Override
    public int compareTo(QuerySearchResObj other) {

        // match score:
        int i = getScore().compareTo(other.getScore());
        if (i != 0) {
            return -i;
        }

        return 0;
    }

}

class SpellSuggestResObj implements Comparable<SpellSuggestResObj> {
    private final String sugg;
    public String getSugg() {
        return sugg;
    }
    public SpellSuggestResObj(
            String sugg
            ) {
        this.sugg = sugg;
    }
    @Override
    public int compareTo(SpellSuggestResObj other) {

        // match score:
        int i = getSugg().compareTo(other.getSugg());
        if (i != 0) {
            return -i;
        }

        return 0;
    }

}

