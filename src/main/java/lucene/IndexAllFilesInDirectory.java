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
public class IndexAllFilesInDirectory {
	static int counter = 0;

	public static void main(String[] args) throws Exception {
		String indexPath = "indexPath";
		String docsPath = "D:\\Mayank\\docsPath2";

		System.out.println("Indexing to directory '" + indexPath + "'...");
		Directory dir = FSDirectory.open(Paths.get(indexPath));

//		Defines the index generation logic
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		IndexWriter writer = new IndexWriter(dir, iwc);
		indexDocs(writer, Paths.get(docsPath));
		writer.close();

	}

	static void indexDocs(final IndexWriter writer, Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				indexDoc(writer, file);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/** Indexes a single document */
	static void indexDoc(IndexWriter writer, Path file) throws IOException {
		InputStream stream = Files.newInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		String title = br.readLine();
		Document doc = new Document();
		doc.add(new StringField("path", file.toString(), Field.Store.YES));
		doc.add(new TextField("contents", title, Field.Store.YES));
		doc.add(new StringField("title", title, Field.Store.YES));
		writer.addDocument(doc);
		counter++;
//		if (counter % 1000 == 0)
		System.out.println("indexing " + counter + "-th file " + file.getFileName());
//		;
	}
}