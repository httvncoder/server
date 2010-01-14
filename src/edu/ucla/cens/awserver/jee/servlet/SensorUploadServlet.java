package edu.ucla.cens.awserver.jee.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import edu.ucla.cens.awserver.controller.Controller;
import edu.ucla.cens.awserver.jee.servlet.glue.AwRequestCreator;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * @author selsky
 */
@SuppressWarnings("serial") 
public class SensorUploadServlet extends HttpServlet {
	private static Logger _logger = Logger.getLogger(SensorUploadServlet.class);
	private Controller _controller;
	private AwRequestCreator _awRequestCreator;
	
	/**
	 * Default no-arg constructor.
	 */
	public SensorUploadServlet() {
		
	}
		
	/**
	 * JavaEE-to-Spring glue code. When the web application starts up, the init method on all servlets is invoked by the Servlet 
	 * container (if load-on-startup for the Servlet > 0). In this method, names of Spring "beans" are pulled out of the 
	 * ServletConfig and the names are used to retrieve the beans out of the ApplicationContext. The basic design rule followed
	 * is that only Servlet.init methods contain Spring Framework glue code.
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String servletName = config.getServletName();
		
		String awRequestCreatorName = config.getInitParameter("awRequestCreatorName");
		
		if(StringUtils.isEmptyOrWhitespaceOnly(awRequestCreatorName)) {
			throw new ServletException("Invalid web.xml. Missing awRequestCreatorName init param. Servlet " + servletName +
					" cannot be initialized and put into service.");
		}
		
		// OK, now get the beans out of the Spring ApplicationContext
		// If the beans do not exist within the Spring configuration, Spring will throw a RuntimeException and initialization
		// of this Servlet will fail. (check catalina.out in addition to aw.log)
		ServletContext servletContext = config.getServletContext();
		ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		
		_awRequestCreator = (AwRequestCreator) applicationContext.getBean(awRequestCreatorName);
		
		
	}
	
	/**
	 * Services the user requests to the URLs bound to this Servlet as configured in web.xml. 
	 * 
	 * Performs the following steps:
	 * <ol>
	 * <li>Maps HTTP request parameters into an AwRequest.
	 * <li>Passes the AwRequest to a Controller.
	 * <li>Places the results of the controller action into the HTTP request.
	 * <li> ... TODO
	 * </ol>
	 */
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException { // allow Tomcat to handle Servlet and IO Exceptions
		
//		// Map data from the inbound request to our internal format
//		AwRequest awRequest = _awRequestCreator.createFrom(request);
//	    
//		try {
//			// Execute feature-specific logic
//			_controller.execute(awRequest);
//		    
//			// Map data from the request into the HttpSession for later use or for rendering within a JSP
//			if(null != _httpSessionModifier) {
//				_httpSessionModifier.modifySession(awRequest, request.getSession());
//			}
//			
//			// Redirect to JSP
//			boolean failedRequest = Boolean.valueOf(((String) awRequest.getPayload().get("failedRequest")));
//			
//			if(failedRequest) {
////				if(awRequest.getPayload().containsKey("errorMessage")) {
////					_logger.info(awRequest.getPayload().get("errorMessage"));
////				}
//				
//				response.sendRedirect(_failedRequestRedirectUrl);
//				
//			} else {
//				
//				response.sendRedirect(_successfulRequestRedirectUrl);
//				// getServletContext().getRequestDispatcher(_successfulRequestRedirectUrl).forward (request, response);
//			}
//		}
//		
//		catch(ControllerException ce) { 
//			
//			_logger.error("", ce); // make sure the stack trace gets into our app log
//			throw ce; // re-throw and allow Tomcat to redirect to the configured error page. the stack trace will also end up
//			          // in catalina.out
//			
//		}
	}
	
	/**
	 * Dispatches to processRequest().
	 */
	@Override protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		processRequest(req, resp);

	}

	/**
	 * Dispatches to processRequest().
	 */
	@Override protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
    
		processRequest(req, resp);
	
	}
	
}