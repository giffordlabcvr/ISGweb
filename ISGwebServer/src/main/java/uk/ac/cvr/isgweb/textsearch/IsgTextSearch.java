package uk.ac.cvr.isgweb.textsearch;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import uk.ac.cvr.isgweb.database.IsgDatabase;

public class IsgTextSearch {

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isg.textsearch");

	private static IsgTextSearch instance;
	
	private Directory index = new RAMDirectory();
	private Analyzer analyzer = new SimpleAnalyzer();
	// private Analyzer analyzer = new GeneNameAnalyzer();

	
	private IsgTextSearch() {
		index(IsgDatabase.getInstance());
	}
	
	public static IsgTextSearch getInstance() {
		if(instance == null) {
			instance = new IsgTextSearch();
		}
		return instance;
	}
	
	private void index(IsgDatabase isgDatabase) {
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter;
		logger.info("Indexing gene names");
		try {
			indexWriter = new IndexWriter(index, config);
		} catch(IOException ioe) {
			throw new RuntimeException("Unexpected IOException: "+ioe.getMessage(), ioe);
		}
		Collection<String> geneNames = isgDatabase.getGeneNameToEnsemblIds().keySet();
		geneNames.forEach(geneName -> {
			try {
				addDoc(indexWriter, geneName);
			} catch(IOException ioe) {
				throw new RuntimeException("Unexpected IOException: "+ioe.getMessage(), ioe);
			}
		});

		try {
			indexWriter.close();
		} catch(IOException ioe) {
			throw new RuntimeException("Unexpected IOException: "+ioe.getMessage(), ioe);
		}
		logger.info("Gene name indexing complete");
	}

	private void addDoc(IndexWriter w, String geneName) throws IOException {
		Document doc = new Document();
		TextField textField = new TextField("geneName", geneName, Field.Store.YES);
 		doc.add(textField);
		w.addDocument(doc);
	}
	
	private ScoreDoc[] searchTitle(IndexSearcher searcher, String queryString, int hitsPerPage) throws ParseException, IOException {
		QueryParser queryParser = new QueryParser("geneName", analyzer);
		queryParser.setAllowLeadingWildcard(true);
		Query q = queryParser.parse("*"+queryString+"*");
		TopDocs docs = searcher.search(q, hitsPerPage);
		return docs.scoreDocs;
	}

	public IndexSearcher getSearcher() throws IOException {
		IndexReader reader = DirectoryReader.open(index);
		return new IndexSearcher(reader);
	}

	public static void main(String[] args) throws Exception {
		IsgTextSearch instance = getInstance();
		IndexSearcher searcher = instance.getSearcher();
		ScoreDoc[] hits = instance.searchTitle(searcher, "OAS", 20);
		System.out.println("Found " + hits.length + " hits.");
		for(int i=0;i<hits.length;++i) {
		    int docId = hits[i].doc;
		    Document d = searcher.doc(docId);
		    System.out.println((i + 1) + ". " + d.get("geneName"));
		}
	}
	
}
