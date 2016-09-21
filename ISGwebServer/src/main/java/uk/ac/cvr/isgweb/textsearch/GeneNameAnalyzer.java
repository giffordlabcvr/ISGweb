package uk.ac.cvr.isgweb.textsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;

public class GeneNameAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
    	Tokenizer source = new LowerCaseTokenizer();
        TokenStream filter = new NGramTokenFilter(source, 3, 6);
        return new TokenStreamComponents(source, filter);
    }
}