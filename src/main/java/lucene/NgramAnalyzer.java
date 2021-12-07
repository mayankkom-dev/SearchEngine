package lucene;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 *
 */
public class NgramAnalyzer extends Analyzer {

    public NgramAnalyzer() {
        super();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        TokenStream tokenStream = new LowerCaseFilter(source);
        tokenStream = new StopFilter(tokenStream, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        tokenStream = new ASCIIFoldingFilter(tokenStream);
        tokenStream = new NGramTokenFilter(tokenStream, 3, 5, true);
        return new TokenStreamComponents(source, tokenStream);
    }

}