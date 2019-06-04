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
package io.gravitee.rest.api.portal.standalone.jetty;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.rest.api.portal.rest.resource.GraviteePortalApplication;
import io.gravitee.rest.api.portal.security.SecurityPortalConfiguration;
import io.gravitee.rest.api.portal.standalone.jetty.handler.NoContentOutputErrorHandler;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class JettyEmbeddedContainer extends AbstractLifecycleComponent<JettyEmbeddedContainer> implements ApplicationContextAware {

    @Autowired
    private Server server;

    private ApplicationContext applicationContext;

    @Value("${http.api.entrypoint:/}")
    private String entrypoint;

    @Override
    protected void doStart() throws Exception {
        AbstractHandler noContentHandler = new NoContentOutputErrorHandler();
        // This part is needed to avoid WARN while starting container.
        noContentHandler.setServer(server);
        server.addBean(noContentHandler);

        // Create the servlet context
        final ServletContextHandler context = new ServletContextHandler(server, entrypoint, ServletContextHandler.SESSIONS);

        // REST configuration for Portal API
        final ServletHolder servletPortalHolder = new ServletHolder(ServletContainer.class);
        servletPortalHolder.setInitParameter("javax.ws.rs.Application", GraviteePortalApplication.class.getName());
        servletPortalHolder.setInitOrder(1);
        context.addServlet(servletPortalHolder, "/portal/*");

        // Spring configuration
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "basic");

        AnnotationConfigWebApplicationContext webApplicationContext = new AnnotationConfigWebApplicationContext();
        webApplicationContext.register(SecurityPortalConfiguration.class);

        webApplicationContext.setEnvironment((ConfigurableEnvironment) applicationContext.getEnvironment());
        webApplicationContext.setParent(applicationContext);

        context.addEventListener(new ContextLoaderListener(webApplicationContext));

        // Spring Security filter
        context.addFilter(new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")),"/*", EnumSet.allOf(DispatcherType.class));

        // start the server
        server.start();
    }

    @Override
    protected void doStop() throws Exception {
        server.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}