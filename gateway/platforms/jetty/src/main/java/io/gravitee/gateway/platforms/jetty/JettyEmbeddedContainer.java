package io.gravitee.gateway.platforms.jetty;

import io.gravity.gateway.api.Platform;
import io.gravitee.gateway.platforms.servlet.DispatcherServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyEmbeddedContainer implements Platform {

	protected final static String REGEX_SERVLET_MAPPING_ALL = "/*";

	private Server server;

	public String name() {
		//TODO: Get Jetty informations somewhere from Jetty artifact
		return "Jetty"; //$NON-NLS-1$
	}

	public void start() throws Exception {
		server = new Server(8082);
		Handler rootHandler = createHandler();

		server.setHandler(rootHandler);
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
	}

	private Handler createHandler() {
		ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		handler.setContextPath("/");

		ServletHolder dispatchServletHolder = new ServletHolder(DispatcherServlet.class);
		handler.addServlet(dispatchServletHolder, REGEX_SERVLET_MAPPING_ALL);

		return handler;
	}
}
