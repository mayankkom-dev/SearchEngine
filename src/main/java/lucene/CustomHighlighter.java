package lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;

import java.io.IOException;

public class CustomHighlighter {

    private static final String PRE_TAG = "<span class=\"hilite\">";
    private static final String POST_TAG = "</span>";
//    Properties props
    public static String highlight(Query query, TopDocs results,
                                   IndexSearcher searcher, Analyzer analyzer, ScoreDoc hit)
            throws IOException, InvalidTokenOffsetsException {
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter(PRE_TAG, POST_TAG);
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
        int id = hit.doc;
        Document doc = searcher.doc(id);

        String text = doc.get("contents");
        String highlightedText = null;

        // let's highlight that text:  
        TokenStream tokenStream = TokenSources.getTokenStream("contents",
                searcher.getIndexReader().getTermVectors(id), text, analyzer, -1);
        TextFragment[] frags = highlighter.getBestTextFragments(tokenStream, text, false, 10);

        for (TextFragment frag : frags) {
            if ((frag != null) && (frag.getScore() > 0)) {
                highlightedText = frag.toString();
            }
        }
        return  highlightedText;
    }

}