package io.gravitee.gateway.platforms.jetty.resource;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.rules.ExternalResource;

import javax.servlet.Servlet;

/**
 * Rule à utiliser pour lancer un serveur Jetty en amont des tests et l'éteindre après la suite de test.
 */
public class ApiExternalResource extends ExternalResource {

  private Server server;

  private final String port;

  private final String path;

  private final Class<?> servletClass;

  private final Class<?>[] filters;

  public ApiExternalResource(String port, Class<? extends Servlet> servletClass, String path, Class<?>[] filters) {
    super();
    this.port = port;
    this.path = path;
    this.servletClass = servletClass;
    this.filters = filters;
  }

  /**
   *
   */
  @Override
  protected void before() throws Throwable {

    // Création du serveur Jetty.
    this.server = new Server(Integer.valueOf(this.port));

    // Initialisation du contexte Spring.
    WebAppContext webapp = new WebAppContext();
    webapp.setContextPath("/");
    webapp.setResourceBase("/");

    webapp.addServlet(this.servletClass.getName(), this.path);
    this.server.setHandler(webapp);
    if (this.filters != null) {
      for (Class<?> c : this.filters) {
        webapp.addFilter(c.getName(), "/*", null);
      }
    }

    try {
      // Démarrage du serveur.
      this.server.start();
    } catch (Exception e) {
      System.err.println("Exception in server of api management proxy.");
      e.printStackTrace();
    } finally {
      if (this.server.isStarted()) {
        System.out.println("Jetty server for api management proxy successfully started.");
      }
    }
  }

  /**
   *
   */
  @Override
  protected void after() {
    try {
      this.server.stop();
    } catch (Exception e) {
      System.err.println(e);
    }
  }

}
