package lucene;//Jianguo Lu, Dec 2016.

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;


public class SearchIndexedDocs {
	public static void main(String[] args) throws Exception {
		int topS=5;
		String index = "indexPathF";
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
//		Analyzer analyzer = new StandardAnalyzer();
		CharArraySet enStopSet = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
		Analyzer analyzer = new StandardAnalyzer(enStopSet);
		QueryParser parser = new QueryParser("contents", analyzer);
		Query query = parser.parse("programming language");
		TopDocs results = searcher.search(query, topS);
		System.out.println(results.totalHits + " total matching documents");
		for (int i = 0; i < topS; i++) {
			Document doc = searcher.doc(results.scoreDocs[i].doc);
			String path = doc.get("path");
			System.out.println((i + 1) + ". " + path);
			String title = doc.get("contents");
			if (title != null) {
//				System.out.println("Title: " + doc.get("title"));
				System.out.println("Highlighted: \n" + CustomHighlighter.highlight(query, results, searcher, analyzer, results.scoreDocs[i]));
			}
		}
		reader.close();
	}
}