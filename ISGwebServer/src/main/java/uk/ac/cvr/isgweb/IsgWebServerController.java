package uk.ac.cvr.isgweb;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import uk.ac.cvr.isgweb.database.GeneRegulationParams;
import uk.ac.cvr.isgweb.database.IsgDatabase;
import uk.ac.cvr.isgweb.database.SpeciesCriterion;
import uk.ac.cvr.isgweb.document.JsonConversions;
import uk.ac.cvr.isgweb.document.JsonUtils;
import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;

public class IsgWebServerController {

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isgweb");
	
	private JsonWriterFactory jsonWriterFactory;
	
	public IsgWebServerController() {
		this.jsonWriterFactory = Json.createWriterFactory(new LinkedHashMap<String, Object>());

	}


	@POST()
	@Path("/queryIsgs")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String queryIsgs(String requestString, @Context HttpServletResponse response) {
		try {
			JsonObject requestObj = JsonUtils.stringToJsonObject(requestString);
			logger.log(Level.INFO, "JSON Request:\n"+JsonUtils.prettyPrint(requestObj));

			JsonObject geneRegulationParamsObj = ((JsonObject) requestObj.get("geneRegulationParams"));
			GeneRegulationParams geneRegulationParams = JsonConversions.geneRegulationParamsFromJsonObj(geneRegulationParamsObj);

			JsonArray speciesCriteriaJsonArray = ((JsonArray) requestObj.get("speciesCriteria"));
			List<SpeciesCriterion> speciesCriteria = JsonConversions.speciesCriteriaFromJsonArray(speciesCriteriaJsonArray);

			IsgDatabase isgDatabase = IsgDatabase.getInstance();
			logger.log(Level.INFO, "Running query....");
			List<OrthoCluster> resultOrthoClusters = isgDatabase.query(speciesCriteria, geneRegulationParams);
			logger.log(Level.INFO, "Query complete");

			StringWriter stringWriter = new StringWriter();
			JsonGenerator jsonGenerator = Json.createGenerator(stringWriter);
			logger.log(Level.INFO, "Converting results to JSON...");
			jsonGenerator.writeStartObject();
			JsonConversions.orthoClustersToJsonArray(jsonGenerator, geneRegulationParams, resultOrthoClusters);
			jsonGenerator.writeEnd();
			jsonGenerator.flush();
			logger.log(Level.INFO, "Results converted");
			
			String resultString = stringWriter.toString();
			addCacheDisablingHeaders(response);
			return resultString;
		} catch(Throwable th) {
			logger.log(Level.SEVERE, "Error during POST /queryIsgs: "+th.getMessage(), th);
			throw th;
		} 
	} 

	
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
