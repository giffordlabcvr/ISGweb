package uk.ac.cvr.isgweb;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import uk.ac.cvr.isgweb.database.Criterion;
import uk.ac.cvr.isgweb.database.Criterion.Presence;
import uk.ac.cvr.isgweb.database.IsgDatabase;
import uk.ac.cvr.isgweb.document.JsonUtils;
import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;
import uk.ac.cvr.isgweb.model.SpeciesGene;

public class IsgWebServerController {

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isgweb");
	
	private JsonWriterFactory jsonWriterFactory;
	
	public IsgWebServerController() {
		this.jsonWriterFactory = Json.createWriterFactory(new LinkedHashMap<String, Object>());

	}


	@SuppressWarnings("unchecked")
	@POST()
	@Path("/queryIsgs")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String queryIsgs(String commandString, @Context HttpServletResponse response) {
		try {
			System.out.println("JSON request string:"+commandString);
			JsonObject requestObj = JsonUtils.stringToJsonObject(commandString);

			JsonArray criteriaJsonArray = ((JsonArray) requestObj.get("criteria"));
			List<Criterion> criteria = criteriaFromJson(criteriaJsonArray);
			
			List<OrthoCluster> resultOrthoClusters = IsgDatabase.getInstance().query(criteria);

			JsonArray resultOrthoClustersArray = orthoClustersToJsonArray(resultOrthoClusters);
			
			JsonObject resultObj = JsonUtils.jsonObjectBuilder()
					.add("orthoClusters", resultOrthoClustersArray)
					.build();

			String resultString = jsonObjectToString(resultObj);
			addCacheDisablingHeaders(response);
			return resultString;
		} catch(Throwable th) {
			logger.log(Level.SEVERE, "Error during POST /queryIsgs: "+th.getMessage(), th);
			throw th;
		} 
	} 

	
	private JsonArray orthoClustersToJsonArray(List<OrthoCluster> orthoClusters) {
		JsonArrayBuilder arrayBuilder = JsonUtils.jsonArrayBuilder();
		orthoClusters.forEach(orthoCluster -> arrayBuilder.add(orthoClusterToJsonObj(orthoCluster)));
		return arrayBuilder.build();
	}

	private JsonObject orthoClusterToJsonObj(OrthoCluster orthoCluster) {
		JsonObjectBuilder objBuilder = JsonUtils.jsonObjectBuilder();
		objBuilder.add("orthoClusterId", orthoCluster.getOrthoClusterID());
		objBuilder.add("speciesToGenes", speciesToGenesToJsonObj(orthoCluster.getSpeciesToGenes()));
		return objBuilder.build();
	}

	private JsonValue speciesToGenesToJsonObj(Map<Species, List<SpeciesGene>> speciesToGenes) {
		JsonObjectBuilder objBuilder = JsonUtils.jsonObjectBuilder();
		speciesToGenes.forEach((species, genes) -> {
			objBuilder.add(species.name(), genesToJsonArray(genes));
		});
		return objBuilder.build();
	}


	private JsonArray genesToJsonArray(List<SpeciesGene> genes) {
		JsonArrayBuilder arrayBuilder = JsonUtils.jsonArrayBuilder();
		genes.forEach(gene -> {
			arrayBuilder.add(geneToJsonObj(gene));
		});
		return arrayBuilder.build();
	}


	private JsonObject geneToJsonObj(SpeciesGene gene) {
		JsonObjectBuilder objBuilder = JsonUtils.jsonObjectBuilder();
		objBuilder.add("ensembleId", gene.getEnsembleId());
		Optional.ofNullable(gene.getGeneName()).ifPresent(v -> objBuilder.add("geneName", v));
		Optional.ofNullable(gene.getAnyHomology()).ifPresent(v -> objBuilder.add("anyHomology", v));
		Optional.ofNullable(gene.getDnDsRatio()).ifPresent(v -> objBuilder.add("dnDsRatio", v));
		Optional.ofNullable(gene.getFdr()).ifPresent(v -> objBuilder.add("fdr", v));
		Optional.ofNullable(gene.getIsDifferentiallyExpressed()).ifPresent(v -> objBuilder.add("isDifferentiallyExpressed", v));
		Optional.ofNullable(gene.getLog2foldChange()).ifPresent(v -> objBuilder.add("log2FoldChange", v));;
		Optional.ofNullable(gene.getPercentIdentity()).ifPresent(v -> objBuilder.add("percIdentity", v));
		return objBuilder.build();
	}


	private List<Criterion> criteriaFromJson(JsonArray criteriaJsonArray) {
		return criteriaJsonArray.stream()
				.map(v -> criterionFromJsonObj((JsonObject) v))
				.collect(Collectors.toList());
	}

	private Criterion criterionFromJsonObj(JsonObject criterionJsonObj) {
		String speciesIdString = ((JsonString) criterionJsonObj.get("speciesId")).getString();
		Species species = Species.valueOf(speciesIdString);

		String presenceString = ((JsonString) criterionJsonObj.get("presence")).getString();
		Presence presence = Presence.valueOf(presenceString);

		Criterion criterion = new Criterion();
		criterion.setSpecies(species);
		criterion.setPresence(presence);
		
		return criterion;
		
	}


	@SuppressWarnings("unchecked")
	@GET()
	@Path("/species")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSpecies(@Context HttpServletResponse response) {
		try {
			JsonArrayBuilder speciesArrayBuilder = JsonUtils.jsonArrayBuilder();
			for(Species species: Species.values()) {
				speciesArrayBuilder.add(JsonUtils.jsonObjectBuilder()
						.add("id", species.name())
						.add("displayName", species.getDisplayLatinName() + " ("+species.getDisplayCommonName()+")")
				);
			}
			
			JsonObject result = JsonUtils.jsonObjectBuilder()
					.add("species", speciesArrayBuilder.build())
					.build();

			String resultString = JsonUtils.prettyPrint(result);
			System.out.println("result: "+resultString);
			addCacheDisablingHeaders(response);
			return resultString;
		} catch(Throwable th) {
			logger.log(Level.SEVERE, "Error during GET /species: "+th.getMessage(), th);
			throw th;
		} 
	} 

	private String jsonObjectToString(JsonObject jsonObject) {
		StringWriter sw = new StringWriter();
		try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(sw)) {
			jsonWriter.writeObject(jsonObject);
		}
		return sw.toString();
	}
	
	
	private void addCacheDisablingHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setHeader("Expires", "0"); // Proxies.
	}
	
}
