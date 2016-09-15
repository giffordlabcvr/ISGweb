package uk.ac.cvr.isgweb.document;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.stream.JsonGenerator;

import uk.ac.cvr.isgweb.database.Criterion;
import uk.ac.cvr.isgweb.database.Criterion.Presence;
import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;
import uk.ac.cvr.isgweb.model.SpeciesGene;

public class JsonConversions {

	public static void orthoClustersToJsonArray(JsonGenerator jsonGenerator, 
			List<OrthoCluster> orthoClusters) {
		jsonGenerator.writeStartArray("orthoClusters");
		orthoClusters.forEach(orthoCluster -> orthoClusterToJsonObj(jsonGenerator, orthoCluster));
		jsonGenerator.writeEnd();
	}

	public static void orthoClusterToJsonObj(JsonGenerator jsonGenerator, OrthoCluster orthoCluster) {
		jsonGenerator.writeStartObject()
			.write("orthoClusterId", orthoCluster.getOrthoClusterID())
			.write("orthoClusterIdShort", orthoCluster.getOrthoClusterIDShort());
		speciesToGenesToJsonObj(jsonGenerator, orthoCluster.getSpeciesToGenes());
		jsonGenerator.writeEnd();
	}

	public static void speciesToGenesToJsonObj(JsonGenerator jsonGenerator, Map<Species, List<SpeciesGene>> speciesToGenes) {
		jsonGenerator.writeStartObject("speciesToGenes");
		speciesToGenes.forEach((species, genes) -> {
			genesToJsonArray(jsonGenerator, species, genes);
		});
		jsonGenerator.writeEnd();
	}

	public static void genesToJsonArray(JsonGenerator jsonGenerator, Species species, List<SpeciesGene> genes) {
		jsonGenerator.writeStartArray(species.name());
		genes.forEach(gene -> geneToJsonArray(jsonGenerator, gene));
		jsonGenerator.writeEnd();
	}

	// use array for this part of the result as this is the inner loop.
	public static void geneToJsonArray(JsonGenerator jsonGenerator, SpeciesGene gene) {
		jsonGenerator.writeStartArray();
		jsonGenerator.write(gene.getEnsembleId());
		addGeneFieldToArray(jsonGenerator, gene.getGeneName());
		addGeneFieldToArray(jsonGenerator, gene.getDnDsRatio());
		addGeneFieldToArray(jsonGenerator, gene.getLog2foldChange());
		addGeneFieldToArray(jsonGenerator, gene.getFdr());
		addGeneFieldToArray(jsonGenerator, gene.getPercentIdentity());
		addGeneFieldToArray(jsonGenerator, gene.getAnyHomology());
		addGeneFieldToArray(jsonGenerator, gene.getIsDifferentiallyExpressed());
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

	public static List<Criterion> criteriaFromJson(JsonArray criteriaJsonArray) {
		return criteriaJsonArray.stream()
				.map(v -> criterionFromJsonObj((JsonObject) v))
				.collect(Collectors.toList());
	}

	public static Criterion criterionFromJsonObj(JsonObject criterionJsonObj) {
		String speciesIdString = ((JsonString) criterionJsonObj.get("speciesId")).getString();
		Species species = Species.valueOf(speciesIdString);
	
		String presenceString = ((JsonString) criterionJsonObj.get("presence")).getString();
		Presence presence = Presence.valueOf(presenceString);
	
		Criterion criterion = new Criterion();
		criterion.setSpecies(species);
		criterion.setPresence(presence);
		
		return criterion;
		
	}

}
