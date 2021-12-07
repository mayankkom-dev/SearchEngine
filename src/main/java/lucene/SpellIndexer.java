package lucene;
//adapted/simplified from luecene.demo.IndexFiles
// Jianguo Lu, Dec 2016

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Index all text files under a directory.
 */
public class SpellIndexer {
	static int counter = 0;

	public static void main(String[] args) throws Exception {

		String spellIndexPath = "spellIdxPath2"; //"C:\\Users\\Manish Kumar\\IdeaProjects\\LuceneSearch\\spellIdxPath";
		String spellIndexDict = "notstp_en_words.txt"; //"C:\\Users\\Manish Kumar\\IdeaProjects\\LuceneSearch\\notstp_en_words.txt";
		System.out.println("Generating Index for spelling" + spellIndexPath + "'...");

		Directory sdir = FSDirectory.open(Paths.get(spellIndexPath));
		PlainTextDictionary txt_dict = new PlainTextDictionary(Paths.get(spellIndexDict));
		SpellChecker checker = new SpellChecker(sdir);

		System.out.print("\nBuilding index from the .txt dictionary took... ");
		long start_time = System.currentTimeMillis();
		checker.indexDictionary(txt_dict, new IndexWriterConfig(new KeywordAnalyzer()), false);
		sdir.close();
		long end_time = System.currentTimeMillis();
		System.out.println((end_time - start_time)/1000 + " seconds.\n");


	}
}