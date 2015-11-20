/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.external;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Rule à utiliser pour lancer un serveur Jetty en amont des tests et l'éteindre après la suite de test.
 */
public class ApiExternalResource extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExternalResource.class);

    private Server server;

    private int port;

    private final String path;

    private final Class<?> servletClass;

    private final Class<?>[] filters;

    public ApiExternalResource(Class<? extends Servlet> servletClass, String path, Class<?>[] filters) {
        super();
        this.path = path;
        this.servletClass = servletClass;
        this.filters = filters;
    }

    /**
     * Returns a free port number on localhost, or -1 if unable to find a free port.
     *
     * @return a free port number on localhost, or -1 if unable to find a free port
     * @since 3.0
     */
    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
        }
        return -1;
    }

    /**
     *
     */
    @Override
    protected void before() throws Throwable {
        this.port = findFreePort();

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
        } catch (final Exception e) {
            LOGGER.error("Exception in server of api management proxy", e);
        } finally {
            if (this.server.isStarted()) {
                LOGGER.info("Jetty server for api management proxy successfully started");
            }
        }
    }

    public int getPort() {
        return this.port;
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
