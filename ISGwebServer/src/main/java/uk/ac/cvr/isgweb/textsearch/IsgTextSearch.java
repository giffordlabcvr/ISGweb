package uk.ac.cvr.isgweb.textsearch;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

	public static final String GENE_NAME_DOC_FIELD = "geneName";
	public static final String ENSEMBL_ID_DOC_FIELD = "ensemblId";

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isg.textsearch");

	private static IsgTextSearch instance;
	
	private Directory geneNameIndex = new RAMDirectory();
	private Directory ensemblIdIndex = new RAMDirectory();
	private Analyzer analyzer;

	
	private IsgTextSearch() {
		this.analyzer = new StandardAnalyzer();
		/*try {
			this.analyzer = CustomAnalyzer.builder()
	                .withTokenizer("standard")
	                //.addTokenFilter("standard")
	                .addTokenFilter("lowercase")     
	                .addTokenFilter("length", "min", "4", "max", "50")
	                .build();
		} catch(IOException ioe) {
			throw new RuntimeException("Unexpected IOException: "+ioe.getMessage(), ioe);
		}*/
		
		IsgDatabase isgDatabase = IsgDatabase.getInstance();
		indexGeneNames(isgDatabase);
		indexEnsemblIds(isgDatabase);
		
		
	}
	
	public static IsgTextSearch getInstance() {
		if(instance == null) {
			instance = new IsgTextSearch();
		}
		return instance;
	}
	
	private void indexGeneNames(IsgDatabase isgDatabase) {
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter;
		logger.info("Indexing gene names");
		try {
			indexWriter = new IndexWriter(geneNameIndex, config);
		} catch(IOException ioe) {
			throw new RuntimeException("Unexpected IOException: "+ioe.getMessage(), ioe);
		}
		Collection<String> geneNames = isgDatabase.getGeneNameToEnsemblIds().keySet();
		geneNames.forEach(geneName -> {
			try {
				addGeneNameDoc(indexWriter, geneName);
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

	private void addGeneNameDoc(IndexWriter w, String geneName) throws IOException {
		Document doc = new Document();
		TextField textField = new TextField(GENE_NAME_DOC_FIELD, geneName, Field.Store.YES);
 		doc.add(textField);
		w.addDocument(doc);
	}


	
	private void indexEnsemblIds(IsgDatabase isgDatabase) {
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter;
		logger.info("Indexing ENSEMBL IDs");
		try {
			indexWriter = new IndexWriter(ensemblIdIndex, config);
		} catch(IOException ioe) {
			throw new RuntimeException("Unexpected IOException: "+ioe.getMessage(), ioe);
		}
		Collection<String> ensemblIds = isgDatabase.getEnsemblIdToSpeciesGenes().keySet();
		ensemblIds.forEach(ensemblId -> {
			try {
				addEnsemblIdDoc(indexWriter, ensemblId);
			} catch(IOException ioe) {
				throw new RuntimeException("Unexpected IOException: "+ioe.getMessage(), ioe);
			}
		});

		try {
			indexWriter.close();
		} catch(IOException ioe) {
			throw new RuntimeException("Unexpected IOException: "+ioe.getMessage(), ioe);
		}
		logger.info("ENSEMBL ID indexing complete");
	}

	private void addEnsemblIdDoc(IndexWriter w, String ensemblId) throws IOException {
		Document doc = new Document();
		TextField textField = new TextField(ENSEMBL_ID_DOC_FIELD, ensemblId, Field.Store.YES);
 		doc.add(textField);
		w.addDocument(doc);
	}


	
	public ScoreDoc[] search(IndexSearcher searcher, String searchDocField, String queryString, int hitsPerPage) {
		if(!queryString.matches("[A-Za-z0-9_-]+")) {
			logger.info("query did not match: "+queryString);
			return new ScoreDoc[0];
		}
		QueryParser queryParser = new QueryParser(searchDocField, analyzer);
		queryParser.setAllowLeadingWildcard(true);
		Query q;
		try {
			q = queryParser.parse("*"+queryString+"*");
		} catch (ParseException e) {
			throw new RuntimeException("Unexpected ParseException: "+e.getMessage(), e);
		}
		TopDocs docs;
		try {
			docs = searcher.search(q, hitsPerPage);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException: "+e.getMessage(), e);
		}
		return docs.scoreDocs;
	}

	private IndexSearcher getSearcher(Directory index) {
		IndexReader reader;
		try {
			reader = DirectoryReader.open(index);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException: "+e.getMessage(), e);
		}
		return new IndexSearcher(reader);
	}

	public IndexSearcher getGeneNameSearcher() {
		return getSearcher(geneNameIndex);
	}

	public IndexSearcher getEnsemblIdSearcher() {
		return getSearcher(ensemblIdIndex);
	}

	
	public static void main(String[] args) throws Exception {
		IsgTextSearch instance = getInstance();
		IndexSearcher searcher = instance.getGeneNameSearcher();
		ScoreDoc[] hits = instance.search(searcher, GENE_NAME_DOC_FIELD, "OAS", 10);
		System.out.println("Found " + hits.length + " hits.");
		for(int i=0;i<hits.length;++i) {
		    int docId = hits[i].doc;
		    Document d = searcher.doc(docId);
		    System.out.println((i + 1) + ". " + d.get(GENE_NAME_DOC_FIELD));
		}

	
	
		IndexSearcher searcher2 = instance.getEnsemblIdSearcher();
		ScoreDoc[] hits2 = instance.search(searcher2, ENSEMBL_ID_DOC_FIELD, "89127", 10);
		System.out.println("Found " + hits2.length + " hits.");
		for(int i=0;i<hits2.length;++i) {
		    int docId = hits2[i].doc;
		    Document d = searcher2.doc(docId);
		    System.out.println((i + 1) + ". " + d.get(ENSEMBL_ID_DOC_FIELD));
		}

	}

	
}
