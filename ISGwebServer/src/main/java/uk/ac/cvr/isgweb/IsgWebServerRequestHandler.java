package uk.ac.cvr.isgweb;

import javax.ws.rs.Path;

@Path("/")
public class IsgWebServerRequestHandler {
	
	@Path("/")
	public Object handleRequest() {
		return new IsgWebServerController();
	}
}
