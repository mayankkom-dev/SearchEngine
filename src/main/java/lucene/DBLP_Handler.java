package lucene;

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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.*;


public class DBLP_Handler extends DefaultHandler {
    // Initializing static values
    public DBLP_Handler() throws Exception {}
    boolean isTitle=false;
    static int MIN_Length=20;
    static Integer titles=0;
    static Integer fetch_err=0;
    static Integer largeTitles=0;
    public static Integer suspicious=0;
    static String  DBLP_FILE = "D:\\Mayank\\IR\\phase2\\xml\\dblp.xml";
    static Random random = new Random();
    static Directory INDEX_DIR;
    static List docStart = Arrays.asList("article", "inproceedings", "proceedings", "book",
            "incollection", "phdthesis", "mastersthesis", "www", "person", "data");
    static String prevqName = "";
    static {
        try {
            INDEX_DIR = FSDirectory.open(Paths.get("D:\\Mayank\\FinalUI\\xmlIndex"+MIN_Length));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static CharArraySet enStopSet = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
    static Analyzer analyzer = new StandardAnalyzer(enStopSet);
    static IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
    static IndexWriter writer;
    static StringBuilder currentValue = new StringBuilder();
    static StringBuilder allValue = new StringBuilder();
    static Document doc=new Document();
    static {
        try {
            writer = new IndexWriter(INDEX_DIR, iwc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static MongoClient mongoClient;
    static String pdf_p;
    static String pdf_cont;

    static {
        try {
            mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static boolean isURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    static DB database = mongoClient.getDB("LuceneIR");
    static DBCollection collection = database.getCollection("xmlParse");
    static String author;
    static String title = "";
    static String journal;
    static String url;
    static DBObject rec;
    static String uuid;

    // Overriding required methods
    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
        // if qName in doc start tag --> instantiate doc writer
        if (docStart.contains(qName.toLowerCase())) {
            uuid = UUID.randomUUID().toString().replace("-", "");
            if(!title.trim().isEmpty()){//Value has been fetched -> ready for db insert
                System.out.println("###################################################################");
                System.out.printf("Processed %s papers", largeTitles);
                Document doc=new Document();

                doc.add(new TextField("title", title, Field.Store.YES));
                doc.add(new StringField("journal", journal, Field.Store.YES));
                doc.add(new StoredField("url", url));
                doc.add(new StoredField("path", pdf_p));
                doc.add(new TextField("content", pdf_cont, Field.Store.YES));

                if (author != null && author.length() > 0) {
                    if(author.charAt(author.length() - 1) == ';') {
                        author = author.substring(0, author.length() - 1);
                        List<String> authList = Arrays.asList(author.split(";"));
                        for (String auth : authList) {
                            doc.add(new StringField("author", currentValue.toString(), Field.Store.YES));
                        }
                    }else{
                        doc.add(new StringField("author", currentValue.toString(), Field.Store.YES));
                    }

                }

                DBObject rec = new BasicDBObject("_id", uuid)
                        .append("author", author)
                        .append("title", title)
                        .append("journal", journal)
                        .append("url", url)
                        .append("pdf_cont", pdf_cont)
                        .append("pdf_p", pdf_p);
                collection.insert(rec);
                title="";
            }


        }
        else{
            if(prevqName.equalsIgnoreCase(qName)){
                //else if prevQName == qName --> keep updating for db,
                // clear current val for doc add
                // reset the tag value
                currentValue.setLength(0);
            }else{
                //else clear keep updating and current val and wait for doc add                // reset the tag value
                currentValue.setLength(0);
                // reset the tag value
                allValue.setLength(0);
            }
        }
        prevqName = qName;
    }

    public void endElement(String uri, String localName,
                           String qName)
            throws SAXException {
        // Add to doc for lucene based on need
//        author|editor|title|booktitle|pages|year|address|journal|volume|number|month|url
//    |ee|cdrom|cite|publisher|note|crossref|isbn|series|school|chapter|publnr
        if (qName.equalsIgnoreCase("author")||qName.equalsIgnoreCase("editor") ) {
                author = allValue.toString();
                System.out.printf("%s : %s%n", qName, currentValue.toString());
//                Better to add docs while updating db to remove inconsistency
//                doc.add(new StringField("author", currentValue.toString(), Field.Store.YES));
        }
        if (qName.equalsIgnoreCase("title")) {
            String title_ = currentValue.toString();
            titles++;
            if( title_.length()>MIN_Length) {
                largeTitles++;
                title = title_;
                System.out.printf("%s : %s%n", qName, title);
//                doc.add(new TextField("title", title, Field.Store.YES));
            }
        }
        if (qName.equalsIgnoreCase("journal")||qName.equalsIgnoreCase("booktitle") ){
            journal = currentValue.toString();
            System.out.printf("%s : %s%n", qName, journal);
//            doc.add(new StringField("journal", journal, Field.Store.YES));
        }
        if (qName.equalsIgnoreCase("ee")){
            String url_ = currentValue.toString();
            if(isURL(url_)){
                url = url_;
                System.out.printf("%s : %s%n", qName, url);
                System.out.println("Fetching and Extracting pdf");
//                doc.add(new StringField("url", url, Field.Store.YES));
                // Read this url to fetch text and parse those too, may be fetch references too
                try {
                    String[] resp = ExtractTextSimple.fetch_extractPdf(url, uuid);
                    pdf_p = resp[0];
                    pdf_cont = resp[1];
                } catch (IOException e) {
//                    make title blank and don't include for indexing
                    title="";
                    fetch_err++;
                    largeTitles--;
                    e.printStackTrace();
                }
            }
        }
    }
    public void characters(char[] ch,
                           int start,
                           int length)
            throws SAXException{
        currentValue.append(ch, start, length);
        if(!allValue.toString().equals("")){
            allValue.append(";");
        }
        allValue.append(ch, start, length);

//        if (isTitle){
//            titles++;
//            String title=new String(ch, start, length);
//            //System.out.println(titles+"\t"+title);
//            if( title.length()>MIN_Length){
//                largeTitles++;
//                Document doc=new Document();
//                doc.add(new TextField("contents", title, Field.Store.YES));
////                doc.add(new Field("contents", title, Field.Store.YES, Field.Index.TOKENIZED));
////                doc.add(new Field("path", title.hashCode()+"_"+random.nextInt(100), Field.Store.YES, Field.Index.UN_TOKENIZED));
//                try {writer.addDocument(doc);}
//                catch (Exception e) {}
//            }
//            isTitle=false;
//        }
    }

//    public static class PrintAllHandlerSax extends DefaultHandler {
//
//        private StringBuilder currentValue = new StringBuilder();
//
//        @Override
//        public void startDocument() {
//            System.out.println("Start Document");
//        }
//
//        @Override
//        public void endDocument() {
//            System.out.println("End Document");
//        }
//
//        @Override
//        public void startElement(
//                String uri,
//                String localName,
//                String qName,
//                Attributes attributes) {
//
//            // reset the tag value
//            currentValue.setLength(0);
//
//            System.out.printf("Start Element : %s%n", qName);
//
////            if (qName.equalsIgnoreCase("staff")) {
////                // get tag's attribute by name
////                String id = attributes.getValue("id");
////                System.out.printf("Staff id : %s%n", id);
////            }
////
////            if (qName.equalsIgnoreCase("salary")) {
////                // get tag's attribute by index, 0 = first attribute
////                String currency = attributes.getValue(0);
////                System.out.printf("Currency :%s%n", currency);
////            }
//
//        }
//
//        @Override
//        public void endElement(String uri,
//                               String localName,
//                               String qName) {
//            System.out.printf("Name : %s%n", currentValue.toString());
//            System.out.printf("End Element : %s%n", qName);
//
////            if (qName.equalsIgnoreCase("name")) {
////                System.out.printf("Name : %s%n", currentValue.toString());
////            }
////
////            if (qName.equalsIgnoreCase("role")) {
////                System.out.printf("Role : %s%n", currentValue.toString());
////            }
////
////            if (qName.equalsIgnoreCase("salary")) {
////                System.out.printf("Salary : %s%n", currentValue.toString());
////            }
////
////            if (qName.equalsIgnoreCase("bio")) {
////                System.out.printf("Bio : %s%n", currentValue.toString());
////            }
//
//        }
//
//        // http://www.saxproject.org/apidoc/org/xml/sax/ContentHandler.html#characters%28char%5B%5D,%20int,%20int%29
//        // SAX parsers may return all contiguous character data in a single chunk,
//        // or they may split it into several chunks
//        @Override
//        public void characters(char ch[], int start, int length) {
//
//            // The characters() method can be called multiple times for a single text node.
//            // Some values may missing if assign to a new string
//
//            // avoid doing this
//            // value = new String(ch, start, length);
//
//            // better append it, works for single or multiple calls
//            currentValue.append(ch, start, length);
//
//        }
//
//    }


    public static void main(String[] args) throws Exception {

//        INDEX_DIR = FSDirectory.open(Paths.get("D:\\Mayank\\FinalUI\\xmlIndex"+MIN_Length));
//        Analyzer analyzer = new StandardAnalyzer();
//        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
//        writer = new IndexWriter(INDEX_DIR, iwc);
        long startTime = System.currentTimeMillis();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {

            SAXParser saxParser = factory.newSAXParser();
            XMLReader reader=saxParser.getXMLReader();
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setFeature("http://xml.org/sax/features/validation", false);

//            saxParser.parse(new File(DBLP_FILE), new DBLP_Handler());
            saxParser.parse(new File(DBLP_FILE), new DBLP_Handler());
            System.out.println( "titles\t"+titles + "\tlargeTitles\t"+largeTitles);
            writer.close();


        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println(elapsedTime);

    }
}
