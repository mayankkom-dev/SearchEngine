package lucene;
//adapted/simplified from luecene.demo.IndexFiles
// Jianguo Lu, Dec 2016

import com.mongodb.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Index all text files under a directory.
 */
public class IndexWholeDataInDirectory {
	static int counter = 0;
	static String indexPath = "indexPathF";
	static String docsPath = "D:\\Mayank\\IR\\phase2\\2";
	static MongoClient mongoClient;

	static {
		try {
			mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	static DB database = mongoClient.getDB("LuceneIR");
	static DBCollection collection = database.getCollection("txtParsed");

	// Maximum number of threads in thread pool
	static final int MAX_T = 512;

	private static class Processor implements Runnable {
		private final Path file;
		private final IndexWriter writer;

		public Processor(IndexWriter writer, Path file) {
			this.file = file;
			this.writer = writer;
		}

		@Override
		public void run() {
			try {
				indexDoc(writer, file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


//	private static void crawlDirectoryAndProcessFiles(IndexWriter writer, File directory, ThreadPoolExecutor executor) {
//		for (File file : directory.listFiles()) {
//			if (file.isDirectory()) {
//				crawlDirectoryAndProcessFiles(writer, file, executor);
//			} else {
//				executor.execute(new Processor(file);
//			}
//		}
//	}
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		System.out.println("Indexing to directory '" + indexPath + "'...");
		Directory dir = FSDirectory.open(Paths.get(indexPath));

		//	Defines the index generation logic
		CharArraySet enStopSet = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
		Analyzer analyzer = new StandardAnalyzer(enStopSet);
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		IndexWriter writer = new IndexWriter(dir, iwc);
		//	Defining Thread Pool
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_T);
		//	Executing Indexing using thread pool
		indexDocs(writer, Paths.get(docsPath), executor);
		//	Calling shutdown
		executor.shutdown();
		//	waiting to close lucene writer object
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			System.out.println("Something");
		}
		//  closing writer
		writer.close();

		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println(elapsedTime);


	}

	static void indexDocs(final IndexWriter writer, Path path, ThreadPoolExecutor executor) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				executor.execute(new Processor(writer, file));
//				indexDoc(writer, file);

				return FileVisitResult.CONTINUE;
			}
		});
	}

	/** Indexes a single document */
	static void indexDoc(IndexWriter writer, Path file) throws IOException {
//		System.out.println("Thread : " + Thread.currentThread().getName() );
		String uuid = UUID.randomUUID().toString().replace("-", "");
		InputStream stream = Files.newInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		String content = br.lines().collect(Collectors.joining());
		Document doc = new Document();
		doc.add(new StoredField("uuid", uuid));
		doc.add(new StoredField("path", file.toString()));
		doc.add(new TextField("contents", content, Field.Store.YES));
		writer.addDocument(doc);
		DBObject rec = new BasicDBObject("_id", uuid)
				.append("path", file.toString())
				.append("contents", content);
		collection.insert(rec);
		counter++;
//		if (counter % 1000 == 0) {
		System.out.println(Thread.currentThread().getName()+ " indexing " + counter + "-th file " + file.getFileName());
//		}
	}
}