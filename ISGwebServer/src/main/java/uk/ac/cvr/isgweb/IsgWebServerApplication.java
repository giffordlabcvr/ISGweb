package uk.ac.cvr.isgweb;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import uk.ac.cvr.isgweb.database.IsgDatabase;

@WebListener
@ApplicationPath("/")
public class IsgWebServerApplication extends ResourceConfig implements ServletContextListener {

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isg.webserver");
	
	public IsgWebServerApplication() {
		super();
    	registerInstances(new IsgWebServerRequestHandler());
    	registerInstances(new IsgWebServerExceptionHandler());
	}


	
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// init the database on startup
		IsgDatabase.getInstance();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
    
    
}