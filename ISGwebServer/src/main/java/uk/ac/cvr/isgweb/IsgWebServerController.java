package uk.ac.cvr.isgweb;

import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

import uk.ac.cvr.isgweb.database.GeneRegulationParams;
import uk.ac.cvr.isgweb.database.IsgDatabase;
import uk.ac.cvr.isgweb.database.SpeciesCriterion;
import uk.ac.cvr.isgweb.document.JsonConversions;
import uk.ac.cvr.isgweb.document.JsonUtils;
import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;
import uk.ac.cvr.isgweb.textsearch.IsgTextSearch;

public class IsgWebServerController {

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isgweb");
	
	@POST()
	@Path("/queryBySpeciesCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String queryBySpeciesCriteria(String requestString, @Context HttpServletResponse response) {
		try {
			JsonObject requestObj = JsonUtils.stringToJsonObject(requestString);
			logger.log(Level.INFO, "JSON Request:\n"+JsonUtils.prettyPrint(requestObj));

			JsonObject geneRegulationParamsObj = ((JsonObject) requestObj.get("geneRegulationParams"));
			GeneRegulationParams geneRegulationParams = JsonConversions.geneRegulationParamsFromJsonObj(geneRegulationParamsObj);

			JsonArray speciesCriteriaJsonArray = ((JsonArray) requestObj.get("speciesCriteria"));
			List<SpeciesCriterion> speciesCriteria = JsonConversions.speciesCriteriaFromJsonArray(speciesCriteriaJsonArray);

			IsgDatabase isgDatabase = IsgDatabase.getInstance();
			logger.log(Level.INFO, "Running query....");
			List<OrthoCluster> resultOrthoClusters = isgDatabase.queryBySpeciesCriteria(speciesCriteria, geneRegulationParams);
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

	
	@POST()
	@Path("/queryByGeneNameOrEnsemblId")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String queryByGeneNameOrEnsemblId(String requestString, @Context HttpServletResponse response) {
		try {
			JsonObject requestObj = JsonUtils.stringToJsonObject(requestString);
			logger.log(Level.INFO, "JSON Request:\n"+JsonUtils.prettyPrint(requestObj));

			String geneNameOrEnsemblId = ((JsonString) requestObj.get("geneNameOrEnsemblId")).getString();

			JsonObject geneRegulationParamsObj = ((JsonObject) requestObj.get("geneRegulationParams"));
			GeneRegulationParams geneRegulationParams = JsonConversions.geneRegulationParamsFromJsonObj(geneRegulationParamsObj);

			IsgDatabase isgDatabase = IsgDatabase.getInstance();
			logger.log(Level.INFO, "Running query....");
			List<OrthoCluster> resultOrthoClusters = isgDatabase.queryByGeneNameOrEnsemblId(geneNameOrEnsemblId);
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

	
	@POST()
	@Path("/suggestGeneOrEnsembl")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String suggestGeneOrEnsembl(String requestString, @Context HttpServletResponse response) {
		try {
			JsonObject requestObj = JsonUtils.stringToJsonObject(requestString);
			logger.log(Level.INFO, "JSON Request:\n"+JsonUtils.prettyPrint(requestObj));

			String queryText = ((JsonString) requestObj.get("queryText")).getString();
			int maxHits = ((JsonNumber) requestObj.get("maxHits")).intValue();
			IsgTextSearch isgTextSearch = IsgTextSearch.getInstance();
			
			IndexSearcher geneNameSearcher = isgTextSearch.getGeneNameSearcher();
			ScoreDoc[] geneNameHits = isgTextSearch.search(geneNameSearcher, IsgTextSearch.GENE_NAME_DOC_FIELD, queryText, maxHits);

			IndexSearcher ensemblIdSearcher = isgTextSearch.getEnsemblIdSearcher();
			ScoreDoc[] ensemblIdHits = isgTextSearch.search(ensemblIdSearcher, IsgTextSearch.ENSEMBL_ID_DOC_FIELD, queryText, maxHits);

			StringWriter stringWriter = new StringWriter();
			JsonGenerator jsonGenerator = Json.createGenerator(stringWriter);
			logger.log(Level.INFO, "Converting results to JSON...");
			jsonGenerator.writeStartObject();
			JsonConversions.hitsToJson(jsonGenerator, geneNameSearcher, geneNameHits, ensemblIdSearcher, ensemblIdHits, maxHits);
			jsonGenerator.writeEnd();
			jsonGenerator.flush();
			logger.log(Level.INFO, "Results converted");
			
			String resultString = stringWriter.toString();
			addCacheDisablingHeaders(response);
			return resultString;
		} catch(Throwable th) {
			logger.log(Level.SEVERE, "Error during POST /suggestGeneOrEnsembl: "+th.getMessage(), th);
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
	
	@POST()
	@Path("/clusterResultsAsFile") 
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String clusterResultsAsFile(String requestString, @Context HttpServletResponse response) {
		try {
			JsonObject requestObj = JsonUtils.stringToJsonObject(requestString);

			FileType fileType = FileType.valueOf(((JsonString) requestObj.get("fileType")).getString());
			LineFeedStyle lineFeedStyle = LineFeedStyle.valueOf(((JsonString) requestObj.get("lineFeedStyle")).getString());
			JsonArray orthoClusters = ((JsonArray) requestObj.getJsonArray("orthoClusters"));

			logger.info(orthoClusters.size()+"POST /clusterResultsAsFile fileType:"+fileType);
			logger.info(orthoClusters.size()+"POST /clusterResultsAsFile lineFeedStyle:"+lineFeedStyle);
			logger.info(orthoClusters.size()+" ortho clusters submitted to POST /clusterResultsAsFile");
			
			StringWriter stringWriter = new StringWriter();
			JsonGenerator jsonGenerator = Json.createGenerator(stringWriter);
			logger.log(Level.INFO, "Converting results to JSON...");
			jsonGenerator.writeStartObject();
			jsonGenerator.write("fileName", "results."+fileType.getExtension());
			jsonGenerator.write("content", orthoClustersToFileContent(orthoClusters, fileType, lineFeedStyle));
			jsonGenerator.write("contentType", "text/plain");
			jsonGenerator.writeEnd();
			jsonGenerator.flush();
			logger.log(Level.INFO, "Results converted");
			
			String resultString = stringWriter.toString();
			addCacheDisablingHeaders(response);
			return resultString;
		} catch(Throwable th) {
			logger.log(Level.SEVERE, "Error during POST /clusterResultsAsFile: "+th.getMessage(), th);
			throw th;
		} 
	}
	
	private String orthoClustersToFileContent(JsonArray orthoClusters, FileType fileType, LineFeedStyle lineFeedStyle) {
		StringBuffer textBuf = new StringBuffer();
		String delimiter = fileType.getDelimiter();

		textBuf.append("Orthologous Cluster ID");
		textBuf.append(delimiter);
		textBuf.append("Species");
		textBuf.append(delimiter);
		textBuf.append("ENSEMBL ID");
		textBuf.append(delimiter);
		textBuf.append("Gene");
		textBuf.append(delimiter);
		textBuf.append("Expression");
		textBuf.append(delimiter);
		textBuf.append("log2 Fold Change"); 
		textBuf.append(delimiter);
		textBuf.append("FDR"); 
		textBuf.append(lineFeedStyle.getEndOfLine());
		
		orthoClusters.forEach(val -> {
			JsonObject clusterJsonObj = (JsonObject) val;
			final String orthoClusterIdShort = clusterJsonObj.getString("orthoClusterIdShort");
			JsonObject speciesToGenes = clusterJsonObj.getJsonObject("speciesToGenes");
			speciesToGenes.forEach((speciesKey, genesVal) -> {
				Species species = Species.valueOf(speciesKey);
				JsonArray genesArray = (JsonArray) genesVal;
				genesArray.forEach(geneVal -> {
					JsonArray geneArray = (JsonArray) geneVal;
					textBuf.append(orthoClusterIdShort);
					textBuf.append(delimiter);
					textBuf.append(species.getDisplayLatinName());
					textBuf.append(delimiter);
					appendString(textBuf, geneArray.get(0)); // ensembl ID
					textBuf.append(delimiter);
					appendString(textBuf, geneArray.get(1)); // gene name
					textBuf.append(delimiter);
					boolean upregulated = geneArray.getBoolean(8);
					boolean downregulated = geneArray.getBoolean(9);
					if(upregulated) {
						textBuf.append("up_regulated");
					} else if(downregulated) {
						textBuf.append("down_regulated");
					} else {
						textBuf.append("not_differentially_expressed");
					}
					textBuf.append(delimiter);
					appendDouble(textBuf, geneArray.get(3)); // log2FC
					textBuf.append(delimiter);
					appendDouble(textBuf, geneArray.get(4)); // FDR
					textBuf.append(lineFeedStyle.getEndOfLine());
				});
			});
		});
		
		return textBuf.toString();
	}

	private void appendString(StringBuffer buf, JsonValue val) {
		if(val == JsonValue.NULL) {
			buf.append("-");
		} else {
			buf.append(((JsonString) val).getString());
		}
	}

	private void appendDouble(StringBuffer buf, JsonValue val) {
		if(val == JsonValue.NULL) {
			buf.append("-");
		} else {
			buf.append(Double.toString(((JsonNumber) val).doubleValue()));
		}
	}

	private enum FileType {
		tab("\t","txt"),
		csv(",","csv");
		
		private String delimiter;
		private String extension;
		private FileType(String delimiter, String extension) {
			this.delimiter = delimiter;
			this.extension = extension;
		}
		public String getDelimiter() {
			return delimiter;
		}
		public String getExtension() {
			return extension;
		}
	}

	private enum LineFeedStyle {
		crlf("\r\n"),
		lf("\n");
		
		private String endOfLine;
		private LineFeedStyle(String endOfLine) {
			this.endOfLine = endOfLine;
		}
		public String getEndOfLine() {
			return endOfLine;
		}
	}

	
	private void addCacheDisablingHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setHeader("Expires", "0"); // Proxies.
	}
	
}
