package uk.ac.cvr.isgweb.document;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

import uk.ac.cvr.isgweb.database.GeneRegulationParams;
import uk.ac.cvr.isgweb.database.SpeciesCriterion;
import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;
import uk.ac.cvr.isgweb.model.SpeciesGene;
import uk.ac.cvr.isgweb.textsearch.IsgTextSearch;

public class JsonConversions {

	public static void orthoClustersToJsonArray(JsonGenerator jsonGenerator, GeneRegulationParams geneRegulationParams, 
			List<OrthoCluster> orthoClusters) {
		jsonGenerator.writeStartArray("orthoClusters");
		orthoClusters.forEach(orthoCluster -> orthoClusterToJsonObj(jsonGenerator, geneRegulationParams, orthoCluster));
		jsonGenerator.writeEnd();
	}

	public static void orthoClusterToJsonObj(JsonGenerator jsonGenerator, GeneRegulationParams geneRegulationParams, OrthoCluster orthoCluster) {
		jsonGenerator.writeStartObject()
			.write("orthoClusterId", orthoCluster.getOrthoClusterID())
			.write("orthoClusterIdShort", orthoCluster.getOrthoClusterIDShort());
		speciesToGenesToJsonObj(jsonGenerator, geneRegulationParams, orthoCluster.getSpeciesToGenes());
		jsonGenerator.writeEnd();
	}

	public static void speciesToGenesToJsonObj(JsonGenerator jsonGenerator, GeneRegulationParams geneRegulationParams, Map<Species, List<SpeciesGene>> speciesToGenes) {
		jsonGenerator.writeStartObject("speciesToGenes");
		speciesToGenes.forEach((species, genes) -> {
			genesToJsonArray(jsonGenerator, geneRegulationParams, species, genes);
		});
		jsonGenerator.writeEnd();
	}

	public static void genesToJsonArray(JsonGenerator jsonGenerator, GeneRegulationParams geneRegulationParams, Species species, List<SpeciesGene> genes) {
		jsonGenerator.writeStartArray(species.name());
		genes.forEach(gene -> geneToJsonArray(jsonGenerator, geneRegulationParams, gene));
		jsonGenerator.writeEnd();
	}

	// use array for this part of the result as this is the inner loop.
	public static void geneToJsonArray(JsonGenerator jsonGenerator, GeneRegulationParams geneRegulationParams, SpeciesGene gene) {
		jsonGenerator.writeStartArray();
		jsonGenerator.write(gene.getEnsembleId());
		addGeneFieldToArray(jsonGenerator, gene.getGeneName());
		addGeneFieldToArray(jsonGenerator, gene.getDnDsRatio());
		addGeneFieldToArray(jsonGenerator, gene.getLog2foldChange());
		addGeneFieldToArray(jsonGenerator, gene.getFdr());
		addGeneFieldToArray(jsonGenerator, gene.getPercentIdentity());
		addGeneFieldToArray(jsonGenerator, gene.getAnyHomology());
		addGeneFieldToArray(jsonGenerator, gene.getIsDifferentiallyExpressed());
		addGeneFieldToArray(jsonGenerator, geneRegulationParams.upregulatedGene(gene));
		addGeneFieldToArray(jsonGenerator, geneRegulationParams.downregulatedGene(gene));
		jsonGenerator.writeEnd();
	}

	private static void addGeneFieldToArray(JsonGenerator jsonGenerator, Double val) {
		if(val == null)  { jsonGenerator.writeNull(); } else { jsonGenerator.write(val); }
	}
	private static void addGeneFieldToArray(JsonGenerator jsonGenerator, Boolean val) {
		if(val == null)  { jsonGenerator.writeNull(); } else { jsonGenerator.write(val); }
	}
	private static void addGeneFieldToArray(JsonGenerator jsonGenerator, String val) {
		if(val == null)  { jsonGenerator.writeNull(); } else { jsonGenerator.write(val); }
	}

	public static List<SpeciesCriterion> speciesCriteriaFromJsonArray(JsonArray criteriaJsonArray) {
		return criteriaJsonArray.stream()
				.map(v -> speciesCriterionFromJsonObj((JsonObject) v))
				.collect(Collectors.toList());
	}

	
	public static SpeciesCriterion speciesCriterionFromJsonObj(JsonObject specReqJsonObj) {
		SpeciesCriterion speciesCriterion = new SpeciesCriterion();
		speciesCriterion.setSpecies(Species.valueOf(specReqJsonObj.getString("species")));
		speciesCriterion.setRequireUpregulated(specReqJsonObj.getBoolean("requireUpregulated"));
		speciesCriterion.setRequireNotUpregulated(specReqJsonObj.getBoolean("requireNotUpregulated"));
		speciesCriterion.setRequireDownregulated(specReqJsonObj.getBoolean("requireDownregulated"));
		speciesCriterion.setRequireNotDownregulated(specReqJsonObj.getBoolean("requireNotDownregulated"));
		speciesCriterion.setRequirePresent(specReqJsonObj.getBoolean("requirePresent"));
		speciesCriterion.setRequireAbsent(specReqJsonObj.getBoolean("requireAbsent"));
		return speciesCriterion;
	}

	public static GeneRegulationParams geneRegulationParamsFromJsonObj(JsonObject queryExprJsonObj) {
		GeneRegulationParams geneRegulationParams = new GeneRegulationParams();
		geneRegulationParams.setUpregulatedMinLog2FoldChange(queryExprJsonObj.getJsonNumber("upregulatedMinLog2FC").doubleValue());
		geneRegulationParams.setDownregulatedMaxLog2FoldChange(queryExprJsonObj.getJsonNumber("downregulatedMaxLog2FC").doubleValue());
		geneRegulationParams.setMaxFDR(queryExprJsonObj.getJsonNumber("maxFDR").doubleValue());
		return geneRegulationParams;
	}

	public static void hitsToJson(JsonGenerator jsonGenerator, 
			IndexSearcher geneNameSearcher, ScoreDoc[] geneNameScoreDocs, 
			IndexSearcher ensemblIdSearcher, ScoreDoc[] ensemblIdScoreDocs, int maxHits) {
		
		int geneNameScoreDocIndex = 0;
		int ensemblIdScoreDocIndex = 0;
		int numHits = 0;
		
		jsonGenerator.writeStartArray("hits");
		while(numHits < maxHits) {
			String docField;
			IndexSearcher searcher;
			ScoreDoc scoreDoc = null;
			
			ScoreDoc geneNameScoreDoc = null;
			ScoreDoc ensemblIdScoreDoc = null;
			
			if(geneNameScoreDocIndex < geneNameScoreDocs.length) {
				geneNameScoreDoc = geneNameScoreDocs[geneNameScoreDocIndex];
			} 
			if(ensemblIdScoreDocIndex < ensemblIdScoreDocs.length) {
				ensemblIdScoreDoc = ensemblIdScoreDocs[ensemblIdScoreDocIndex];
			} 
			if(geneNameScoreDoc == null) {
				if(ensemblIdScoreDoc == null) {
					break;
				} else {
					scoreDoc = ensemblIdScoreDoc;
					searcher = ensemblIdSearcher;
					docField = IsgTextSearch.ENSEMBL_ID_DOC_FIELD;
					ensemblIdScoreDocIndex++;
				}
			} else {
				if(ensemblIdScoreDoc == null) {
					scoreDoc = geneNameScoreDoc;
					searcher = geneNameSearcher;
					docField = IsgTextSearch.GENE_NAME_DOC_FIELD;
					geneNameScoreDocIndex++;
				} else {
					if(geneNameScoreDoc.score >= ensemblIdScoreDoc.score) {
						scoreDoc = geneNameScoreDoc;
						searcher = geneNameSearcher;
						docField = IsgTextSearch.GENE_NAME_DOC_FIELD;
						geneNameScoreDocIndex++;
					} else {
						scoreDoc = ensemblIdScoreDoc;
						searcher = ensemblIdSearcher;
						docField = IsgTextSearch.ENSEMBL_ID_DOC_FIELD;
						ensemblIdScoreDocIndex++;
					}
				}
			}
			Document document;
			try {
				document = searcher.doc(scoreDoc.doc);
			} catch (IOException ioe) {
				throw new RuntimeException("Unexpected IO exception: "+ioe, ioe);
			}
			jsonGenerator.write(document.get(docField));
			numHits++;
		}
		jsonGenerator.writeEnd();
	}

	
	
}
