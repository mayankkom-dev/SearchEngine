package querySearchServices;

import org.apache.logging.log4j.LogManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.Directory;
import lucene.NgramAnalyzer;
import org.apache.lucene.store.FSDirectory;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class QuerySearchResult {

    private static final String NGRAM_INDEX_PATH =  "ngindexPathF";
    private static final Analyzer NGRAM_ANALYZER = new NgramAnalyzer();
    private static final String SIMPLE_INDEX_PATH = "indexPathF";
    private static final Analyzer SIMPLE_ANALYZER = new StandardAnalyzer();// include stop words
    private static final String spellIndexPath = "spellIdxPath2";
    public static org.apache.logging.log4j.Logger LOGGER = LogManager.getRootLogger();

    public static List<SpellSuggestResObj> spellsuggest(String searchTerm)
            throws IOException, ParseException {
        Directory sdir = FSDirectory.open(Paths.get(spellIndexPath));
        SpellChecker checker = new SpellChecker(sdir);
        System.out.println("Loading SpellCheckerIndex ");
        checker.setSpellIndex(sdir);
        sdir.close();
        // Searching and presenting the suggested words by selecting a string distance
        checker.setStringDistance(new JaroWinklerDistance());
//        checker.setStringDistance(new LevenshteinDistance());
//        checker.setStringDistance(new LuceneLevenshteinDistance());
//        checker.setStringDistance(new NGramDistance());
        String[] suggestions = checker.suggestSimilar(searchTerm, 5);
        List<SpellSuggestResObj> results = new ArrayList();
        for (String sugs : suggestions) {
            results.add(new SpellSuggestResObj(sugs));
        }
//        Collections.sort(results);
        return results;
    }


        public static List<QuerySearchResObj> searchIndex(String searchTerm, boolean ngramSrch, boolean wildCard)
            throws IOException, ParseException {
        String indexPath;
        Analyzer analyzer;
        Query query;
        List<QuerySearchResObj> results = new ArrayList();
        searchTerm = searchTerm.toLowerCase();
        if (ngramSrch) {
            indexPath = NGRAM_INDEX_PATH;
            analyzer = NGRAM_ANALYZER;
            if (wildCard){
                query = new WildcardQuery(new Term("contents", searchTerm));
            }else {
                query = getQueryNgram(searchTerm, analyzer);
            }
        } else {
            indexPath = SIMPLE_INDEX_PATH;
            analyzer = SIMPLE_ANALYZER;
            if (wildCard){
                query = new WildcardQuery(new Term("contents", searchTerm));
            }else {
                query = getQueryStd(searchTerm, analyzer);
            }
        }

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);

        LOGGER.info(String.format("Lucene search using:\n  Analyzer: [%s]\n  Term    : [%s]\n  Query   : [%s]",
                analyzer.getClass().getSimpleName(), searchTerm, query.toString()));

        TopDocs hitsD = searcher.search(query, 100);
        ScoreDoc[] hits = hitsD.scoreDocs;

        UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, analyzer);
        String[] fragments = highlighter.highlight("contents", query, hitsD);
        int i = 0;

        for (ScoreDoc hit : hits) {
            BigDecimal score = new BigDecimal(String.valueOf(hit.score))
                    .setScale(2, RoundingMode.HALF_EVEN);
            results.add(new QuerySearchResObj(searcher.doc(hit.doc).get("path"),
                    fragments[i] , score));
            i=i+1;
        }
        Collections.sort(results);
        return results;
    }

    private static Query getQueryNgram(String searchTerm, Analyzer analyzer)
            throws IOException, ParseException {

        QueryParser parser = new QueryParser("contents", analyzer);
        return parser.parse(QueryParser.escape(searchTerm));

    }

    private static Query getQueryStd(String searchTerm, Analyzer analyzer)
            throws IOException, ParseException {
        QueryParser parser = new QueryParser("contents", analyzer);
        return parser.parse(QueryParser.escape(searchTerm));
    }
}
