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
package io.gravitee.management.standalone.jetty;

import io.gravitee.management.api.resource.GraviteeApplication;
import io.gravitee.management.standalone.jetty.handler.NoContentOutputErrorHandler;
import io.gravitee.management.standalone.spring.StandaloneConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author Titouan COMPIEGNE
 */
public final class JettyEmbeddedContainer {

    private JettyEmbeddedContainer() {
    }

    public static void main(String[] args) throws Exception {
    	
        // Create the embedded server
        final Server server = new Server(8059);

        AbstractHandler noContentHandler = new NoContentOutputErrorHandler();
        // This part is needed to avoid WARN while starting container.
        noContentHandler.setServer(server);
        server.addBean(noContentHandler);

        // Create the servlet context
        final ServletContextHandler context = new ServletContextHandler(server, "/*", ServletContextHandler.SESSIONS);
        
        // REST configuration
        final ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
        servletHolder.setInitParameter("javax.ws.rs.Application", GraviteeApplication.class.getName());
        servletHolder.setInitOrder(0);
        context.addServlet(servletHolder, "/rest/*");
        
        // Spring configuration
        // You can switch according to your security implementation (Basic, OAuth2, ...)
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "basic-auth");
        context.addEventListener(new ContextLoaderListener());
        context.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
        context.setInitParameter("contextConfigLocation", StandaloneConfiguration.class.getName());

        // Spring Security filter
        context.addFilter(new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")),"/*", EnumSet.allOf(DispatcherType.class));
        
        // start the server
        server.start();
        server.join();
    }
}