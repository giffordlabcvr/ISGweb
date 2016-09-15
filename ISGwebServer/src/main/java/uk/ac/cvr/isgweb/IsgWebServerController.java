package uk.ac.cvr.isgweb;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import uk.ac.cvr.isgweb.document.JsonUtils;

public class IsgWebServerController {

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isgweb");
	
	public IsgWebServerController() {
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

			String someQuestion = ((JsonString) requestObj.get("someQuestion")).getString();

			JsonObject result = JsonUtils.jsonObjectBuilder()
					.add("theAnswer", someQuestion)
					.add("someAnswer", "foo")
					.build();

			String commandResult = JsonUtils.prettyPrint(result);
			System.out.println("commandResult: "+commandResult);
			addCacheDisablingHeaders(response);
			return commandResult;
		} catch(Throwable th) {
			logger.log(Level.SEVERE, "Error during queryIsgs: "+th.getMessage(), th);
			throw th;
		} 
	} 

	private void addCacheDisablingHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setHeader("Expires", "0"); // Proxies.
	}
	
}
